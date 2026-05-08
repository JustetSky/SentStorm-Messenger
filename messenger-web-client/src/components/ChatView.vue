<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import { watch, ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useMessageStore } from '@/stores/message'
import MessageList from '@/components/MessageList.vue'
import { webSocketService } from '@/services/websocket'
import api from '@/api/api'
import UserProfile from './UserProfile.vue'
import { useUserStore } from '@/stores/user'
import EmojiPicker from './EmojiPicker.vue'

const chatStore = useChatStore()
const messageStore = useMessageStore()
const userStore = useUserStore()
const newMessage = ref('')
const isSending = ref(false)
const isLoadingMore = ref(false)
const hasMoreMessages = ref(true)
const currentPage = ref(0)
const showPartnerProfile = ref(false)
const partnerInfo = ref<any>(null)
const showEmojiPicker = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)

const fileInputRef = ref<HTMLInputElement | null>(null)
const uploadingImage = ref(false)
const pendingImage = ref<{ file: File; preview: string; imageId: string } | null>(null)

let observer: IntersectionObserver | null = null
let statusInterval: number | null = null

const chatTitle = computed(() => {
  if (!chatStore.activeChatId) return ''
  return chatStore.getChatTitle(chatStore.activeChatId)
})

const partnerStatus = computed(() => {
  if (!partnerInfo.value) return ''
  return formatLastSeen(partnerInfo.value.lastSeen)
})

const isPartnerOnline = computed(() => {
  if (!partnerInfo.value?.lastSeen) return false
  const lastSeen = new Date(partnerInfo.value.lastSeen)
  const now = new Date()
  const diffSeconds = (now.getTime() - lastSeen.getTime()) / 1000
  return diffSeconds < 60
})

function formatLastSeen(lastSeen: string | undefined): string {
  if (!lastSeen) return ''

  const date = new Date(lastSeen)
  const now = new Date()

  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const messageDateTime = messageDate.getTime()

  const diffSeconds = (now.getTime() - date.getTime()) / 1000
  const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })

  if (diffSeconds < 60) return 'Online'
  if (diffSeconds < 3600) return `Last seen ${Math.floor(diffSeconds / 60)} min ago`

  if (messageDateTime === today.getTime()) {
    return `Last seen today at ${timeStr}`
  }

  if (messageDateTime === yesterday.getTime()) {
    return `Last seen yesterday at ${timeStr}`
  }

  return `Last seen ${date.toLocaleDateString()} at ${timeStr}`
}

async function refreshPartnerStatus() {
  if (!chatStore.activeChat?.partnerPublicId) return

  try {
    const freshUser = await api.get(`/users/${chatStore.activeChat.partnerPublicId}`)
    partnerInfo.value = freshUser.data
    userStore.userCache.set(freshUser.data.id, freshUser.data)
    userStore.userCache.set(freshUser.data.publicId, freshUser.data)
  } catch {
    // ignore
  }
}

async function loadPartnerInfo() {
  if (!chatStore.activeChat) return

  const partnerPublicId = chatStore.activeChat.partnerPublicId
  if (partnerPublicId) {
    try {
      const freshUser = await api.get(`/users/${partnerPublicId}`)
      partnerInfo.value = freshUser.data
      userStore.userCache.set(freshUser.data.id, freshUser.data)
      userStore.userCache.set(freshUser.data.publicId, freshUser.data)
    } catch {
      // ignore
    }
  }
}

function openPartnerProfile() {
  loadPartnerInfo()
  showPartnerProfile.value = true
}

function closeChat() {
  chatStore.setActiveChat(null)
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (showEmojiPicker.value) {
      showEmojiPicker.value = false
    } else {
      closeChat()
    }
  }
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.emoji-picker-wrapper') && !target.closest('.emoji-btn')) {
    showEmojiPicker.value = false
  }
}

function insertEmoji(emoji: string) {
  newMessage.value += emoji
  showEmojiPicker.value = false
  inputRef.value?.focus()
}

function toggleEmojiPicker() {
  showEmojiPicker.value = !showEmojiPicker.value
}

function triggerImageUpload() {
  fileInputRef.value?.click()
}

async function createThumbnail(file: File): Promise<string> {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')!

        const size = 48
        canvas.width = size
        canvas.height = size

        const min = Math.min(img.width, img.height)
        const sx = (img.width - min) / 2
        const sy = (img.height - min) / 2
        ctx.drawImage(img, sx, sy, min, min, 0, 0, size, size)

        resolve(canvas.toDataURL('image/jpeg', 0.7))
      }
      img.src = e.target?.result as string
    }
    reader.readAsDataURL(file)
  })
}

async function handleImageSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    alert('Only images are allowed')
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    alert('Image size must be less than 10 MB')
    return
  }

  const preview = URL.createObjectURL(file)
  const thumbnail = await createThumbnail(file)
  const imageId = crypto.randomUUID()
  localStorage.setItem(`thumb_${imageId}`, thumbnail)

  pendingImage.value = { file, preview, imageId }
}

function cancelImage() {
  if (pendingImage.value) {
    URL.revokeObjectURL(pendingImage.value.preview)
    if (pendingImage.value.imageId) {
      localStorage.removeItem(`thumb_${pendingImage.value.imageId}`)
    }
    pendingImage.value = null
  }
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

async function sendImageMessage() {
  if (!pendingImage.value || !chatStore.activeChatId) return

  uploadingImage.value = true

  try {
    const formData = new FormData()
    formData.append('image', pendingImage.value.file)

    const res = await api.post('/messages/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    const { fileUrl, fileName, fileSize } = res.data

    const imageMessage = JSON.stringify({
      type: 'IMAGE',
      url: fileUrl,
      name: fileName,
      size: fileSize,
      thumbId: pendingImage.value.imageId
    })

    await messageStore.sendMessage(chatStore.activeChatId, imageMessage)
    cancelImage()

  } catch {
    alert('Failed to send image')
  } finally {
    uploadingImage.value = false
  }
}

async function handlePaste(event: ClipboardEvent) {
  const items = event.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.type.startsWith('image/')) {
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        const preview = URL.createObjectURL(file)
        const thumbnail = await createThumbnail(file)
        const imageId = crypto.randomUUID()
        localStorage.setItem(`thumb_${imageId}`, thumbnail)
        pendingImage.value = { file, preview, imageId }
      }
      break
    }
  }
}

function setupMessageObserver() {
  observer = new IntersectionObserver(
    (entries) => {
      const visibleMessages = entries
        .filter(entry => entry.isIntersecting)
        .map(entry => {
          const element = entry.target as HTMLElement
          return element.dataset.messageId
        })
        .filter(id => id)

      if (visibleMessages.length > 0) {
        messageStore.markVisibleMessagesAsRead(visibleMessages as string[])
      }
    },
    { threshold: 0.5 }
  )
}

function observeMessages() {
  if (!observer) return
  const messageElements = document.querySelectorAll('[data-message-id]')
  messageElements.forEach(el => observer?.observe(el))
}

function scrollToBottom() {
  const el = document.querySelector('.messages')
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function handleScroll(event: Event) {
  const target = event.target as HTMLElement
  if (!target) return

  if (target.scrollTop < 50 && !isLoadingMore.value && hasMoreMessages.value) {
    await loadMoreMessages()
  }
}

async function loadMoreMessages() {
  if (!chatStore.activeChatId || isLoadingMore.value || !hasMoreMessages.value) return

  isLoadingMore.value = true
  const scrollContainer = document.querySelector('.messages')
  const oldScrollHeight = scrollContainer?.scrollHeight || 0

  try {
    currentPage.value++
    const res = await api.get(`/chats/${chatStore.activeChatId}/messages`, {
      params: { page: currentPage.value, size: 20 }
    })

    const newMessages = res.data.items
    if (newMessages.length === 0) {
      hasMoreMessages.value = false
    } else {
      messageStore.messages = [
        ...newMessages.reverse(),
        ...messageStore.messages
      ]
    }
  } catch {
    // ignore
  } finally {
    isLoadingMore.value = false

    await nextTick()
    if (scrollContainer) {
      const newScrollHeight = scrollContainer.scrollHeight
      scrollContainer.scrollTop = newScrollHeight - oldScrollHeight
    }
    observeMessages()
  }
}

async function sendMessage() {
  const text = newMessage.value.trim()
  if (!text || !chatStore.activeChatId || isSending.value) return

  isSending.value = true

  try {
    await messageStore.sendMessage(chatStore.activeChatId, text)
    newMessage.value = ''

    await nextTick()
    scrollToBottom()
  } catch {
    alert('Failed to send message. Please try again.')
  } finally {
    isSending.value = false
  }
}

function handleKeyPress(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

onMounted(async () => {
  try {
    await webSocketService.connect()
    setupMessageObserver()
    window.addEventListener('keydown', handleKeyDown)
    document.addEventListener('click', handleClickOutside)
    document.addEventListener('paste', handlePaste)

    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible' && chatStore.activeChatId) {
        refreshPartnerStatus()
      }
    })
  } catch {
    // ignore
  }
})

onUnmounted(() => {
  webSocketService.disconnect()
  window.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('paste', handlePaste)
  if (observer) {
    observer.disconnect()
  }
  if (statusInterval) {
    clearInterval(statusInterval)
  }
  if (pendingImage.value) {
    URL.revokeObjectURL(pendingImage.value.preview)
  }
})

watch(
  () => chatStore.activeChatId,
  async (chatId, oldChatId) => {
    // Если чат закрыли — очищаем и выходим
    if (!chatId) {
      messageStore.clear()
      return
    }

    if (oldChatId) {
      webSocketService.unsubscribeFromChat(oldChatId)
    }

    // Очищаем ПЕРЕД загрузкой нового чата
    messageStore.clear()
    currentPage.value = 0
    hasMoreMessages.value = true

    await messageStore.fetchMessages(chatId)
    await refreshPartnerStatus()

    if (webSocketService.isConnected()) {
      webSocketService.subscribeToChat(chatId)
    }

    await nextTick()
    scrollToBottom()
    observeMessages()

    if (statusInterval) clearInterval(statusInterval)
    statusInterval = window.setInterval(refreshPartnerStatus, 30000)
  },
  { immediate: true }
)

watch(
  () => messageStore.messages.length,
  async () => {
    await nextTick()
    observeMessages()
  }
)
</script>

<template>
  <div class="chat-container">
    <div v-if="chatStore.activeChat" class="chat">
      <div class="header" @click="openPartnerProfile">
        <div class="header-avatar" :class="{ online: isPartnerOnline }">
          {{ chatTitle.split(' ').map(w => w[0]).join('').slice(0, 2) }}
          <span v-if="isPartnerOnline" class="online-indicator"></span>
        </div>
        <div class="header-info">
          <span class="header-title">{{ chatTitle }}</span>
          <span v-if="partnerStatus" class="header-status">{{ partnerStatus }}</span>
        </div>
        <button class="close-chat-btn" @click.stop="closeChat" title="Close chat (ESC)">
          ×
        </button>
      </div>

      <div class="messages" @scroll="handleScroll">
        <div v-if="isLoadingMore" class="loading-more">
          Loading...
        </div>
        <MessageList />
      </div>

      <div class="input-wrapper">
        <div class="input-container">
          <div v-if="pendingImage" class="image-preview">
            <img :src="pendingImage.preview" alt="Preview" />
            <div class="image-preview-info">
              <span>{{ pendingImage.file.name }}</span>
              <span>{{ (pendingImage.file.size / 1024).toFixed(1) }} KB</span>
            </div>
            <button class="cancel-image-btn" @click="cancelImage" :disabled="uploadingImage">×</button>
            <button class="send-image-btn" @click="sendImageMessage" :disabled="uploadingImage">
              {{ uploadingImage ? 'Sending...' : 'Send' }}
            </button>
          </div>

          <div class="input">
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleImageSelect"
            />

            <button class="attach-btn" @click="triggerImageUpload" title="Attach image">
              🖼️
            </button>

            <input
              ref="inputRef"
              v-model="newMessage"
              placeholder="Message"
              @keypress="handleKeyPress"
              :disabled="isSending || uploadingImage"
            />

            <button class="emoji-btn" @click="toggleEmojiPicker" title="Add emoji">
              🙂
            </button>

            <button class="send-btn" @click="sendMessage" :disabled="isSending || uploadingImage || (!newMessage.trim() && !pendingImage)">
              {{ isSending || uploadingImage ? '...' : '➤' }}
            </button>
          </div>

          <div v-if="showEmojiPicker" class="emoji-picker-wrapper">
            <EmojiPicker @select="insertEmoji" />
          </div>
        </div>
      </div>
    </div>

    <div v-else class="no-chat">
      <div class="no-chat-content">
        <div class="no-chat-icon">💬</div>
        <b>Select a chat</b>
      </div>
    </div>

    <UserProfile
      :show="showPartnerProfile"
      :user="partnerInfo"
      @close="showPartnerProfile = false"
    />
  </div>
</template>

<style scoped>
.chat-container {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
}

.chat {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.header {
  height: 60px;
  background: white;
  border-bottom: 1px solid #e6e9ef;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  flex-shrink: 0;
  cursor: pointer;
  transition: background 0.15s;
}

.header:hover {
  background: #f9fafb;
}

.header-avatar {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
  flex-shrink: 0;
}

.online-indicator {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 10px;
  height: 10px;
  background: #22c55e;
  border: 2px solid white;
  border-radius: 50%;
}

.header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  font-weight: 600;
  font-size: 15px;
  color: #111827;
}

.header-status {
  font-size: 12px;
  color: #9ca3af;
}

.close-chat-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: none;
  font-size: 24px;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.close-chat-btn:hover {
  background: #f1f3f6;
  color: #111827;
}

.messages {
  flex: 1;
  background: #f7f8fa;
  overflow-y: auto;
  padding-top: 16px;
  position: relative;
}

.loading-more {
  text-align: center;
  padding: 10px;
  color: #9ca3af;
  font-size: 12px;
}

.input-wrapper {
  display: flex;
  justify-content: center;
  padding: 12px;
  background: white;
  border-top: 1px solid #e6e9ef;
  flex-shrink: 0;
}

.input-container {
  position: relative;
  width: 100%;
  max-width: 800px;
}

.input {
  display: flex;
  gap: 8px;
  align-items: center;
}

.input input {
  flex: 1;
  height: 42px;
  border-radius: 20px;
  border: none;
  background: #f1f3f6;
  padding: 0 16px;
  outline: none;
  min-width: 0;
}

.attach-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: #f1f3f6;
  color: #6b7280;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.attach-btn:hover {
  background: #e5e7eb;
}

.emoji-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: #f1f3f6;
  color: #6b7280;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.emoji-btn:hover {
  background: #e5e7eb;
}

.send-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: #3a76f0;
  color: white;
  cursor: pointer;
  flex-shrink: 0;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.image-preview {
  position: absolute;
  bottom: 70px;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 10;
  max-width: 100%;
  box-sizing: border-box;
  margin: 0 auto;
}

.image-preview img {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.image-preview-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #111827;
  overflow: hidden;
}

.image-preview-info span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.image-preview-info span:last-child {
  font-size: 11px;
  color: #6b7280;
}

.cancel-image-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f1f3f6;
  color: #6b7280;
  font-size: 18px;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cancel-image-btn:hover {
  background: #e5e7eb;
}

.send-image-btn {
  padding: 8px 16px;
  border-radius: 20px;
  border: none;
  background: #3a76f0;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  white-space: nowrap;
}

.send-image-btn:hover {
  background: #2b5ec9;
}

.send-image-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.emoji-picker-wrapper {
  position: absolute;
  bottom: 60px;
  right: 0;
  z-index: 100;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  border-radius: 12px;
  overflow: hidden;
}

.no-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
}

.no-chat-content {
  text-align: center;
  color: #9ca3af;
}

.no-chat-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

@media (max-width: 600px) {
  .image-preview {
    flex-wrap: wrap;
    padding: 10px;
  }

  .image-preview-info {
    flex-basis: 100%;
    order: 2;
  }

  .send-image-btn {
    flex: 1;
    text-align: center;
  }

  .input {
    gap: 4px;
  }

  .input input {
    padding: 0 12px;
    font-size: 14px;
  }
}

@media (max-width: 400px) {
  .header {
    padding: 0 12px;
  }

  .header-title {
    font-size: 14px;
  }

  .input input {
    padding: 0 10px;
    font-size: 13px;
  }

  .attach-btn,
  .emoji-btn,
  .send-btn {
    width: 38px;
    height: 38px;
  }
}
</style>
