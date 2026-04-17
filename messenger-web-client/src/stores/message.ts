import { defineStore } from 'pinia'
import api from '@/api/api'
import { useUserStore } from './user'
import { useChatStore } from './chat'
import { cryptoService } from '@/services/crypto'
import { deviceService } from '@/services/device'

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
    readTimeouts: new Map<string, number>()
  }),

  actions: {
    async fetchMessages(chatId: string) {
      this.loading = true

      try {
        const res = await api.get(`/chats/${chatId}/messages?page=0&size=50`)

        const messages = await Promise.all(res.data.items.map(async (msg: Message) => {
          try {
            if (msg.ciphertext && msg.ciphertext.startsWith('{') && msg.ciphertext.includes('senderDeviceId')) {
              const decryptedText = cryptoService.decryptFromSender(msg.ciphertext)
              return { ...msg, ciphertext: decryptedText }
            }
            return msg
          } catch {
            return { ...msg, ciphertext: '[Cannot decrypt]' }
          }
        }))

        this.messages = messages.sort((a: Message, b: Message) =>
          new Date(a.createdDate).getTime() - new Date(b.createdDate).getTime()
        )

        await this.markMessagesAsRead(chatId)
      } finally {
        this.loading = false
      }
    },

    async markMessagesAsRead(_chatId: string) {
      const userStore = useUserStore()
      const currentUserId = userStore.profile?.id

      if (!currentUserId) return

      const unreadMessages = this.messages.filter(msg =>
        msg.senderId !== currentUserId &&
        msg.state !== 'READ'
      )

      for (const msg of unreadMessages) {
        try {
          if (msg.state === 'SENT') {
            await api.patch(`/messages/${msg.id}/delivered`)
            msg.state = 'DELIVERED'
          }

          await api.patch(`/messages/${msg.id}/read`)
          msg.state = 'READ'
        } catch {
          // ignore
        }
      }
    },

    async sendMessage(chatId: string, plaintext: string, clientMessageId?: string) {
      const userStore = useUserStore()
      const chatStore = useChatStore()
      const messageId = clientMessageId || crypto.randomUUID()

      if (!userStore.profile?.id) {
        throw new Error('User profile not loaded')
      }

      const chat = chatStore.chats.find(c => c.chatId === chatId)
      if (!chat?.partnerPublicId) {
        throw new Error('Recipient not found')
      }

      const recipientDevices = await deviceService.getRecipientDevices(chat.partnerPublicId)

      if (!recipientDevices || recipientDevices.length === 0) {
        throw new Error('Recipient has no active devices')
      }

      const ciphertext = cryptoService.encryptForAllDevices(plaintext, recipientDevices)

      let messageType = 'TEXT'
      try {
        const parsed = JSON.parse(plaintext)
        if (parsed.type === 'IMAGE') {
          messageType = 'IMAGE'
        }
      } catch {
        // ignore
      }

      const optimisticMessage: Message = {
        id: `pending-${messageId}`,
        clientMessageId: messageId,
        senderId: userStore.profile.id,
        ciphertext: plaintext,
        type: messageType,
        state: 'SENDING',
        createdDate: new Date().toISOString()
      }

      this.messages.push(optimisticMessage)
      this.pendingMessages.set(messageId, optimisticMessage)

      try {
        const response = await api.post('/messages', {
          chatId,
          ciphertext,
          clientMessageId: messageId,
          type: messageType
        })

        const realMessage = response.data

        const decryptedText = cryptoService.decryptFromSender(realMessage.ciphertext)

        const index = this.messages.findIndex(m => m.clientMessageId === messageId)
        if (index !== -1) {
          this.messages[index] = {
            ...realMessage,
            ciphertext: decryptedText,
            state: 'SENT'
          }
        }

        this.pendingMessages.delete(messageId)
        return realMessage
      } catch (error) {
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

      const exists = this.messages.some(m =>
        m.id === message.id ||
        (m.clientMessageId && m.clientMessageId === message.clientMessageId)
      )

      if (!exists) {
        try {
          if (message.ciphertext && message.ciphertext.startsWith('{') && message.ciphertext.includes('senderDeviceId')) {
            const decryptedText = cryptoService.decryptFromSender(message.ciphertext)
            message.ciphertext = decryptedText
          }
        } catch {
          message.ciphertext = '[Cannot decrypt]'
        }

        this.messages.push(message)
        this.messages.sort((a, b) =>
          new Date(a.createdDate).getTime() - new Date(b.createdDate).getTime()
        )

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
      } catch {
        // ignore
      }
    },

    async markVisibleMessagesAsRead(messageIds: string[]) {
      const userStore = useUserStore()
      const currentUserId = userStore.profile?.id

      if (!currentUserId) return

      for (const msgId of messageIds) {
        const message = this.messages.find(m => m.id === msgId)
        if (message && message.senderId !== currentUserId && message.state !== 'READ') {
          try {
            await api.patch(`/messages/${msgId}/read`)
            message.state = 'READ'
          } catch {
            // ignore
          }
        }
      }
    },

    deleteMessage(messageId: string) {
      const index = this.messages.findIndex(m => m.id === messageId)
      if (index !== -1) {
        this.messages.splice(index, 1)
      }
    },

    updateMessageStatus(messageId: string, status: string) {
      const message = this.messages.find(m => m.id === messageId)
      if (message) {
        message.state = status
      }
    },

    clear() {
      this.readTimeouts.forEach(timeout => clearTimeout(timeout))
      this.readTimeouts.clear()
      this.messages = []
      this.pendingMessages.clear()
    }
  }
})
