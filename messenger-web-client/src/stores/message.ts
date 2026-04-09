import { defineStore } from 'pinia'
import api from '@/api/api'
import { useUserStore } from './user'

export interface Message {
  id: string
  clientMessageId: string
  senderId: string
  ciphertext: string
  type: string
  state: string
  createdDate: string
}

export const useMessageStore = defineStore('message', {
  state: () => ({
    messages: [] as Message[],
    loading: false,
    pendingMessages: new Map<string, Message>(),
    readTimeouts: new Map<string, number>() // Для debounce отметки прочтения
  }),

  actions: {
    async fetchMessages(chatId: string) {
      this.loading = true

      try {
        const res = await api.get(`/chats/${chatId}/messages?page=0&size=50`)
        // Сортируем по дате (старые сверху)
        this.messages = res.data.items.sort((a: Message, b: Message) =>
          new Date(a.createdDate).getTime() - new Date(b.createdDate).getTime()
        )

        // Отмечаем непрочитанные сообщения как доставленные и прочитанные
        await this.markMessagesAsRead(chatId)
      } finally {
        this.loading = false
      }
    },

    async markMessagesAsRead(chatId: string) {
      const userStore = useUserStore()
      const currentUserId = userStore.profile?.id

      if (!currentUserId) return

      // Находим все сообщения от других пользователей, которые не прочитаны
      const unreadMessages = this.messages.filter(msg =>
        msg.senderId !== currentUserId &&
        msg.state !== 'READ'
      )

      // Отмечаем их как delivered и read
      for (const msg of unreadMessages) {
        try {
          // Сначала отмечаем как доставленное
          if (msg.state === 'SENT') {
            await api.patch(`/messages/${msg.id}/delivered`)
            msg.state = 'DELIVERED'
          }

          // Затем как прочитанное
          await api.patch(`/messages/${msg.id}/read`)
          msg.state = 'READ'
        } catch (error) {
          console.error('Failed to mark message as read:', error)
        }
      }
    },

    async sendMessage(chatId: string, ciphertext: string, clientMessageId?: string) {
      const userStore = useUserStore()
      const messageId = clientMessageId || crypto.randomUUID()

      if (!userStore.profile?.id) {
        throw new Error('User profile not loaded')
      }

      // Оптимистичное добавление сообщения
      const optimisticMessage: Message = {
        id: `pending-${messageId}`,
        clientMessageId: messageId,
        senderId: userStore.profile.id,
        ciphertext,
        type: 'TEXT',
        state: 'SENDING',
        createdDate: new Date().toISOString()
      }

      this.messages.push(optimisticMessage)
      this.pendingMessages.set(messageId, optimisticMessage)

      try {
        const response = await api.post('/messages', {
          chatId,
          ciphertext,
          clientMessageId: messageId
        })

        const realMessage = response.data

        // Заменяем оптимистичное сообщение реальным
        const index = this.messages.findIndex(m => m.clientMessageId === messageId)
        if (index !== -1) {
          this.messages[index] = {
            ...realMessage,
            state: 'SENT' // Сервер возвращает SENT
          }
        }

        this.pendingMessages.delete(messageId)

        return realMessage
      } catch (error) {
        // Помечаем сообщение как ошибочное
        const index = this.messages.findIndex(m => m.clientMessageId === messageId)
        if (index !== -1) {
          this.messages[index] = {
            ...this.messages[index],
            state: 'ERROR'
          } as Message
        }

        this.pendingMessages.delete(messageId)
        throw error
      }
    },

    addMessage(message: Message) {
      const userStore = useUserStore()

      // Проверяем, нет ли уже такого сообщения
      const exists = this.messages.some(m =>
        m.id === message.id ||
        (m.clientMessageId && m.clientMessageId === message.clientMessageId)
      )

      if (!exists) {
        this.messages.push(message)
        // Сортируем сообщения по дате
        this.messages.sort((a, b) =>
          new Date(a.createdDate).getTime() - new Date(b.createdDate).getTime()
        )

        // Если сообщение от другого пользователя, отмечаем как delivered
        if (message.senderId !== userStore.profile?.id) {
          this.markMessageAsDelivered(message.id)
        }
      }
    },

    async markMessageAsDelivered(messageId: string) {
      try {
        await api.patch(`/messages/${messageId}/delivered`)
        const message = this.messages.find(m => m.id === messageId)
        if (message && message.state === 'SENT') {
          message.state = 'DELIVERED'
        }
      } catch (error) {
        console.error('Failed to mark as delivered:', error)
      }
    },

    async markVisibleMessagesAsRead() {
      const userStore = useUserStore()
      const currentUserId = userStore.profile?.id

      if (!currentUserId) return

      // Находим все видимые сообщения от других пользователей
      const visibleMessages = this.messages.filter(msg =>
        msg.senderId !== currentUserId &&
        msg.state !== 'READ'
      )

      // Отмечаем каждое как прочитанное
      for (const msg of visibleMessages) {
        try {
          await api.patch(`/messages/${msg.id}/read`)
          msg.state = 'READ'
        } catch (error) {
          console.error('Failed to mark as read:', error)
        }
      }
    },

    updateMessageStatus(messageId: string, status: string) {
      const message = this.messages.find(m => m.id === messageId)
      if (message) {
        console.log(`Updating message ${messageId} status: ${message.state} -> ${status}`)
        message.state = status
      }
    },

    clear() {
      // Очищаем тайм-ауты
      this.readTimeouts.forEach(timeout => clearTimeout(timeout))
      this.readTimeouts.clear()

      this.messages = []
      this.pendingMessages.clear()
    }
  }
})
