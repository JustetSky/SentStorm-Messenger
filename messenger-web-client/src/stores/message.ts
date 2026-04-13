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
      console.log('📥 Fetching messages for chat:', chatId)

      try {
        const res = await api.get(`/chats/${chatId}/messages?page=0&size=50`)
        console.log(`📥 Received ${res.data.items.length} messages`)

        const messages = await Promise.all(res.data.items.map(async (msg: Message) => {
          try {
            if (msg.ciphertext && msg.ciphertext.startsWith('{') && msg.ciphertext.includes('senderDeviceId')) {
              console.log(`🔓 Decrypting message ${msg.id}...`)
              const decryptedText = cryptoService.decryptFromSender(msg.ciphertext)
              return { ...msg, ciphertext: decryptedText }
            }
            console.log(`📝 Message ${msg.id} is plaintext or old format`)
            return msg
          } catch (error) {
            console.error(`❌ Failed to decrypt message ${msg.id}:`, error)
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

    async markMessagesAsRead(chatId: string) {
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
        } catch (error) {
          console.error('Failed to mark message as read:', error)
        }
      }
    },

    async sendMessage(chatId: string, plaintext: string, clientMessageId?: string) {
      const userStore = useUserStore()
      const chatStore = useChatStore()
      const messageId = clientMessageId || crypto.randomUUID()

      console.log('📤 ========== SENDING MESSAGE ==========')
      console.log('📝 Plaintext:', plaintext)
      console.log('💬 ChatId:', chatId)

      if (!userStore.profile?.id) {
        throw new Error('User profile not loaded')
      }

      const chat = chatStore.chats.find(c => c.chatId === chatId)
      if (!chat?.partnerPublicId) {
        throw new Error('Recipient not found')
      }

      console.log('👤 Recipient publicId:', chat.partnerPublicId)

      const recipientDevices = await deviceService.getRecipientDevices(chat.partnerPublicId)
      console.log('📱 Recipient devices:', recipientDevices)

      if (!recipientDevices || recipientDevices.length === 0) {
        console.error('❌ Recipient has no active devices')
        throw new Error('Recipient has no active devices')
      }

      const ciphertext = cryptoService.encryptForAllDevices(plaintext, recipientDevices)

      const optimisticMessage: Message = {
        id: `pending-${messageId}`,
        clientMessageId: messageId,
        senderId: userStore.profile.id,
        ciphertext: plaintext,
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
        console.log('✅ Message saved on server:', realMessage.id)

        const decryptedText = cryptoService.decryptFromSender(realMessage.ciphertext)
        console.log('✅ Message decrypted for display')

        const index = this.messages.findIndex(m => m.clientMessageId === messageId)
        if (index !== -1) {
          this.messages[index] = {
            ...realMessage,
            ciphertext: decryptedText,
            state: 'SENT'
          }
        }

        this.pendingMessages.delete(messageId)
        console.log('📤 ========================================')
        return realMessage
      } catch (error) {
        console.error('❌ Failed to send message:', error)
        const index = this.messages.findIndex(m => m.clientMessageId === messageId)
        if (index !== -1) {
          this.messages[index] = {
            ...this.messages[index],
            state: 'ERROR'
          } as Message
        }

        this.pendingMessages.delete(messageId)
        console.log('📤 ========================================')
        throw error
      }
    },

    addMessage(message: Message) {
      const userStore = useUserStore()

      console.log('📨 ========== RECEIVING MESSAGE ==========')
      console.log('📨 Message ID:', message.id)

      const exists = this.messages.some(m =>
        m.id === message.id ||
        (m.clientMessageId && m.clientMessageId === message.clientMessageId)
      )

      if (!exists) {
        try {
          if (message.ciphertext && message.ciphertext.startsWith('{') && message.ciphertext.includes('senderDeviceId')) {
            console.log('🔓 Decrypting received message...')
            const decryptedText = cryptoService.decryptFromSender(message.ciphertext)
            message.ciphertext = decryptedText
            console.log('✅ Message decrypted')
          } else {
            console.log('📝 Message is plaintext or old format')
          }
        } catch (error) {
          console.error('❌ Failed to decrypt received message:', error)
          message.ciphertext = '[Cannot decrypt]'
        }

        this.messages.push(message)
        this.messages.sort((a, b) =>
          new Date(a.createdDate).getTime() - new Date(b.createdDate).getTime()
        )

        if (message.senderId !== userStore.profile?.id) {
          this.markMessageAsDelivered(message.id)
        }
        console.log('📨 ========================================')
      } else {
        console.log('⚠️ Message already exists, skipping')
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
          } catch (error) {
            console.error('Failed to mark as read:', error)
          }
        }
      }
    },

    deleteMessage(messageId: string) {
      const index = this.messages.findIndex(m => m.id === messageId)
      if (index !== -1) {
        this.messages.splice(index, 1)
        console.log('🗑️ Message deleted:', messageId)
      }
    },

    updateMessageStatus(messageId: string, status: string) {
      const message = this.messages.find(m => m.id === messageId)
      if (message) {
        console.log(`📊 Message ${messageId} status: ${message.state} -> ${status}`)
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
