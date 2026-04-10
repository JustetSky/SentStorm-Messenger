<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import { watch, ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useMessageStore } from '@/stores/message'
import MessageList from '@/components/MessageList.vue'
import { webSocketService } from '@/services/websocket'
import api from '@/api/api'
import UserProfile from './UserProfile.vue'
import { useUserStore } from '@/stores/user'

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

let observer: IntersectionObserver | null = null

const chatTitle = computed(() => {
  if (!chatStore.activeChatId) return ''
  return chatStore.getChatTitle(chatStore.activeChatId)
})

// Загружаем информацию о партнере для профиля
async function loadPartnerInfo() {
  if (!chatStore.activeChat) return

  const partnerPublicId = chatStore.activeChat.partnerPublicId
  if (partnerPublicId) {
    try {
      partnerInfo.value = await userStore.fetchUser(partnerPublicId)
    } catch (error) {
      console.error('Failed to load partner info:', error)
    }
  }
}

// Открыть профиль собеседника
function openPartnerProfile() {
  loadPartnerInfo()
  showPartnerProfile.value = true
}

// Выход из чата
function closeChat() {
  chatStore.setActiveChat('')
}

// Обработчик ESC
function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeChat()
  }
}

onMounted(async () => {
  try {
    await webSocketService.connect()
    setupMessageObserver()
    window.addEventListener('keydown', handleKeyDown)
  } catch (error) {
    console.error('Failed to connect WebSocket:', error)
  }
})

onUnmounted(() => {
  webSocketService.disconnect()
  window.removeEventListener('keydown', handleKeyDown)
  if (observer) {
    observer.disconnect()
  }
})

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
        markVisibleMessagesAsRead(visibleMessages as string[])
      }
    },
    {
      threshold: 0.5
    }
  )
}

function observeMessages() {
  if (!observer) return
  const messageElements = document.querySelectorAll('[data-message-id]')
  messageElements.forEach(el => observer?.observe(el))
}

async function markVisibleMessagesAsRead(messageIds: string[]) {
  const currentUserId = userStore.profile?.id

  for (const msgId of messageIds) {
    const message = messageStore.messages.find(m => m.id === msgId)
    if (message && message.senderId !== currentUserId && message.state !== 'READ') {
      try {
        await api.patch(`/messages/${msgId}/read`)
        message.state = 'READ'
      } catch (error) {
        console.error('Failed to mark as read:', error)
      }
    }
  }
}

watch(
  () => chatStore.activeChatId,
  async (chatId, oldChatId) => {
    if (!chatId) return

    if (oldChatId) {
      webSocketService.unsubscribeFromChat(oldChatId)
    }

    messageStore.clear()
    currentPage.value = 0
    hasMoreMessages.value = true
    await messageStore.fetchMessages(chatId)

    if (webSocketService.isConnected()) {
      webSocketService.subscribeToChat(chatId)
    }

    await nextTick()
    scrollToBottom()
    observeMessages()
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
      params: {
        page: currentPage.value,
        size: 20
      }
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
  } catch (error) {
    console.error('Failed to load more messages:', error)
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
  } catch (error) {
    console.error('Failed to send message:', error)
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
</script>

<template>
  <div class="chat-container">
    <div v-if="chatStore.activeChat" class="chat">
      <!-- HEADER с кликом для профиля -->
      <div class="header" @click="openPartnerProfile">
        <div class="header-avatar">
          {{ chatTitle.split(' ').map(w => w[0]).join('').slice(0, 2) }}
        </div>
        <span class="header-title">{{ chatTitle }}</span>
        <button class="close-chat-btn" @click.stop="closeChat" title="Close chat (ESC)">
          ×
        </button>
      </div>

      <!-- MESSAGES -->
      <div class="messages" @scroll="handleScroll">
        <div v-if="isLoadingMore" class="loading-more">
          Loading...
        </div>
        <MessageList />
      </div>

      <!-- INPUT -->
      <div class="input-wrapper">
        <div class="input">
          <input
            v-model="newMessage"
            placeholder="Message"
            @keypress="handleKeyPress"
            :disabled="isSending"
          />
          <button @click="sendMessage" :disabled="isSending || !newMessage.trim()">
            {{ isSending ? '...' : '➤' }}
          </button>
        </div>
      </div>
    </div>

    <div v-else class="no-chat">
      <div class="no-chat-content">
        <div class="no-chat-icon">💬</div>
        <b>Select a chat</b>
      </div>
    </div>

    <!-- Профиль собеседника -->
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

.header-title {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
  color: #111827;
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

.input {
  width: 100%;
  max-width: 800px;
  display: flex;
  gap: 10px;
}

.input input {
  flex: 1;
  height: 42px;
  border-radius: 20px;
  border: none;
  background: #f1f3f6;
  padding: 0 16px;
  outline: none;
}

.input button {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: #3a76f0;
  color: white;
  cursor: pointer;
}

.input button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.no-chat-content p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}
</style>
