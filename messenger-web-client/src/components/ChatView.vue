<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import { watch, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useMessageStore } from '@/stores/message'
import MessageList from '@/components/MessageList.vue'
import { webSocketService } from '@/services/websocket'
import api from '@/api/api'
import { computed } from 'vue'

const chatStore = useChatStore()
const messageStore = useMessageStore()
const newMessage = ref('')
const isSending = ref(false)
const isLoadingMore = ref(false)
const hasMoreMessages = ref(true)
const currentPage = ref(0)

// Используем IntersectionObserver для отметки прочтения
let observer: IntersectionObserver | null = null

const chatTitle = computed(() => {
  if (!chatStore.activeChatId) return ''
  return chatStore.getChatTitle(chatStore.activeChatId)
})

onMounted(async () => {
  try {
    await webSocketService.connect()
    setupMessageObserver()
  } catch (error) {
    console.error('Failed to connect WebSocket:', error)
  }
})

onUnmounted(() => {
  webSocketService.disconnect()
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
        // Отмечаем видимые сообщения как прочитанные
        markVisibleMessagesAsRead(visibleMessages as string[])
      }
    },
    {
      threshold: 0.5 // Сообщение считается видимым если 50% видно
    }
  )
}

function observeMessages() {
  if (!observer) return

  // Наблюдаем за всеми сообщениями
  const messageElements = document.querySelectorAll('[data-message-id]')
  messageElements.forEach(el => observer?.observe(el))
}

async function markVisibleMessagesAsRead(messageIds: string[]) {
  const userStore = useUserStore()
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
    observeMessages() // Начинаем наблюдение за сообщениями
  },
  { immediate: true }
)

// Наблюдаем за новыми сообщениями
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

// Импорт в конце для избежания циклических зависимостей
import { useUserStore } from '@/stores/user'
</script>

<template>
  <div class="chat-container">
    <div v-if="chatStore.activeChat" class="chat">
      <!-- HEADER -->
      <div class="header">
        {{ chatTitle }}
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
      Select a chat
    </div>
  </div>
</template>

<style scoped>
/* Стили остаются без изменений */
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
  font-weight: 600;
  flex-shrink: 0;
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
  margin: auto;
  color: #9ca3af;
}
</style>
