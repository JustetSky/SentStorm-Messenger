import { defineStore } from 'pinia'
import api from '@/api/api'
import { useUserStore } from './user'
import { cryptoService } from '@/services/crypto'

export interface Chat {
  chatId: string
  lastMessageId: string | null
  lastMessageCiphertext: string | null
  lastMessageTime: string | null
  title?: string
  partnerId?: string
  partnerPublicId?: string
}

export const useChatStore = defineStore('chat', {
  state: () => ({
    chats: [] as Chat[],
    activeChatId: null as string | null,
    chatPartners: new Map<string, { id: string, publicId: string, firstName: string, lastName: string }>()
  }),

  getters: {
    activeChat: (state) => {
      return state.chats.find(c => c.chatId === state.activeChatId)
    },

    getChatTitle: (state) => {
      return (chatId: string): string => {
        const chat = state.chats.find(c => c.chatId === chatId)
        if (!chat) return 'Unknown'

        const partnerInfo = state.chatPartners.get(chatId)
        if (partnerInfo) {
          return `${partnerInfo.firstName} ${partnerInfo.lastName}`
        }

        return chat.title || 'Chat'
      }
    },

    findChatByPartnerId: (state) => {
      return (partnerId: string): Chat | undefined => {
        return state.chats.find(c => c.partnerId === partnerId)
      }
    },

    findChatByPublicId: (state) => {
      return (publicId: string): Chat | undefined => {
        return state.chats.find(c => c.partnerPublicId === publicId)
      }
    }
  },

  actions: {
    getStorageKey(): string {
      const userStore = useUserStore()
      const userId = userStore.profile?.id
      if (!userId) return 'chatPartners_anonymous'
      return `chatPartners_${userId}`
    },

    loadSavedPartners() {
      try {
        const key = this.getStorageKey()
        const saved = localStorage.getItem(key)
        if (saved) {
          const partners = JSON.parse(saved)
          this.chatPartners = new Map(Object.entries(partners))
        } else {
          this.chatPartners.clear()
        }
      } catch {
        this.chatPartners.clear()
      }
    },

    savePartners() {
      try {
        const key = this.getStorageKey()
        const obj = Object.fromEntries(this.chatPartners)
        localStorage.setItem(key, JSON.stringify(obj))
      } catch {
        // ignore
      }
    },

    clearCache() {
      this.chats = []
      this.activeChatId = null
      this.chatPartners.clear()
    },

    async fetchChats() {
      this.loadSavedPartners()

      const res = await api.get('/chats')

      this.chats = await Promise.all(res.data.map(async (chat: any) => {
        const other = chat.otherParticipant

        let decryptedLastMessage: string | null = null
        if (chat.lastMessageCiphertext) {
          try {
            if (chat.lastMessageCiphertext.startsWith('{') && chat.lastMessageCiphertext.includes('senderDeviceId')) {
              decryptedLastMessage = cryptoService.decryptFromSender(chat.lastMessageCiphertext)
            } else {
              decryptedLastMessage = chat.lastMessageCiphertext
            }
          } catch {
            decryptedLastMessage = '[Encrypted message]'
          }
        }

        if (other) {
          this.chatPartners.set(chat.chatId, {
            id: other.userId,
            publicId: other.publicId,
            firstName: other.firstName,
            lastName: other.lastName
          })

          return {
            chatId: chat.chatId,
            lastMessageId: chat.lastMessageId || null,
            lastMessageCiphertext: decryptedLastMessage,
            lastMessageTime: chat.lastMessageTime || null,
            title: `${other.firstName} ${other.lastName}`,
            partnerId: other.userId,
            partnerPublicId: other.publicId
          }
        }

        return {
          chatId: chat.chatId,
          lastMessageId: chat.lastMessageId || null,
          lastMessageCiphertext: decryptedLastMessage,
          lastMessageTime: chat.lastMessageTime || null,
          title: 'Chat'
        }
      }))

      this.savePartners()
    },

    async createOrGetChat(publicId: string) {
      const userStore = useUserStore()
      const user = await userStore.fetchUser(publicId)

      // Проверяем, есть ли уже чат с этим пользователем в списке
      const existingChat = this.chats.find(c =>
        c.partnerPublicId === user.publicId
      )

      if (existingChat) {
        this.chatPartners.set(existingChat.chatId, {
          id: user.id,
          publicId: user.publicId,
          firstName: user.firstName,
          lastName: user.lastName
        })
        this.savePartners()
        existingChat.title = `${user.firstName} ${user.lastName}`
        return existingChat
      }

      // Создаём новый чат
      try {
        const res = await api.post('/chats', { userPublicId: publicId })
        const newChat = res.data
        const chatId = newChat.chatId || newChat.id

        // Проверяем, не вернул ли сервер существующий чат (которого нет в нашем списке)
        const alreadyExists = this.chats.find(c => c.chatId === chatId)
        if (alreadyExists) {
          // Обновляем данные существующего чата
          this.chatPartners.set(chatId, {
            id: user.id,
            publicId: user.publicId,
            firstName: user.firstName,
            lastName: user.lastName
          })
          this.savePartners()
          alreadyExists.title = `${user.firstName} ${user.lastName}`
          alreadyExists.partnerId = user.id
          alreadyExists.partnerPublicId = user.publicId
          return alreadyExists
        }

        // Добавляем новый чат
        this.chatPartners.set(chatId, {
          id: user.id,
          publicId: user.publicId,
          firstName: user.firstName,
          lastName: user.lastName
        })
        this.savePartners()

        const chatToAdd: Chat = {
          chatId: chatId,
          lastMessageId: null,
          lastMessageCiphertext: null,
          lastMessageTime: null,
          title: `${user.firstName} ${user.lastName}`,
          partnerId: user.id,
          partnerPublicId: user.publicId
        }

        this.chats.unshift(chatToAdd)
        return chatToAdd
      } catch (error) {
        throw error
      }
    },

    setChatPartner(chatId: string, user: any) {
      this.chatPartners.set(chatId, {
        id: user.id,
        publicId: user.publicId,
        firstName: user.firstName,
        lastName: user.lastName
      })
      this.savePartners()

      const chat = this.chats.find(c => c.chatId === chatId)
      if (chat) {
        chat.title = `${user.firstName} ${user.lastName}`
        chat.partnerId = user.id
        chat.partnerPublicId = user.publicId
      }
    },

    setActiveChat(chatId: string | null) {
      this.activeChatId = chatId
    },

    deleteChat(chatId: string) {
      const index = this.chats.findIndex(c => c.chatId === chatId)
      if (index !== -1) {
        this.chats.splice(index, 1)
      }
      if (this.activeChatId === chatId) {
        this.activeChatId = null
      }
      this.chatPartners.delete(chatId)
      this.savePartners()
    },

    updateLastMessage(chatId: string, message: any) {
      const chatIndex = this.chats.findIndex(c => c.chatId === chatId)
      if (chatIndex === -1) return

      const chat = this.chats[chatIndex]
      if (!chat) return

      chat.lastMessageId = message.id

      try {
        if (message.ciphertext && message.ciphertext.startsWith('{') && message.ciphertext.includes('senderDeviceId')) {
          chat.lastMessageCiphertext = cryptoService.decryptFromSender(message.ciphertext)
        } else {
          chat.lastMessageCiphertext = message.ciphertext
        }
      } catch {
        chat.lastMessageCiphertext = '[Encrypted message]'
      }

      chat.lastMessageTime = message.createdDate

      if (chatIndex > 0) {
        const reorderedChats = [...this.chats]
        const movedChat = reorderedChats.splice(chatIndex, 1)[0]
        if (movedChat) {
          reorderedChats.unshift(movedChat)
          this.chats = reorderedChats
        }
      }
    },

    updateLastMessageFromServer(chatId: string, lastMessage: any) {
      const chat = this.chats.find(c => c.chatId === chatId)
      if (!chat) return

      if (lastMessage) {
        chat.lastMessageId = lastMessage.id
        try {
          if (lastMessage.ciphertext && lastMessage.ciphertext.startsWith('{') && lastMessage.ciphertext.includes('senderDeviceId')) {
            chat.lastMessageCiphertext = cryptoService.decryptFromSender(lastMessage.ciphertext)
          } else {
            chat.lastMessageCiphertext = lastMessage.ciphertext
          }
        } catch {
          chat.lastMessageCiphertext = '[Encrypted message]'
        }
        chat.lastMessageTime = lastMessage.createdDate
      } else {
        chat.lastMessageId = null
        chat.lastMessageCiphertext = null
        chat.lastMessageTime = null
      }
    }
  }
})
