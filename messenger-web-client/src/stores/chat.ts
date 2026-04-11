import {defineStore} from 'pinia'
import api from '@/api/api'
import {useUserStore} from './user'

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
    // Получаем ключ для localStorage с учетом пользователя
    getStorageKey(): string {
      const userStore = useUserStore()
      const userId = userStore.profile?.id
      if (!userId) return 'chatPartners_anonymous'
      return `chatPartners_${userId}`
    },

    // Загружаем сохраненные данные о партнерах из localStorage
    loadSavedPartners() {
      try {
        const key = this.getStorageKey()
        const saved = localStorage.getItem(key)
        if (saved) {
          const partners = JSON.parse(saved)
          this.chatPartners = new Map(Object.entries(partners))
          console.log('Loaded partners from:', key, partners)
        } else {
          this.chatPartners.clear()
          console.log('No saved partners for:', key)
        }
      } catch (e) {
        console.error('Failed to load saved partners:', e)
        this.chatPartners.clear()
      }
    },

    // Сохраняем данные о партнерах в localStorage
    savePartners() {
      try {
        const key = this.getStorageKey()
        const obj = Object.fromEntries(this.chatPartners)
        localStorage.setItem(key, JSON.stringify(obj))
        console.log('Saved partners to:', key, obj)
      } catch (e) {
        console.error('Failed to save partners:', e)
      }
    },

    // Очищаем кеш при смене пользователя
    clearCache() {
      this.chats = []
      this.activeChatId = null
      this.chatPartners.clear()
    },

    async fetchChats() {
      // Загружаем сохраненных партнеров при старте
      this.loadSavedPartners()

      const res = await api.get('/chats')
      console.log('Fetched chats:', res.data)

      this.chats = res.data.map((chat: any) => {
        const other = chat.otherParticipant

        if (other) {
          // Сохраняем в chatPartners
          this.chatPartners.set(chat.chatId, {
            id: other.userId,
            publicId: other.publicId,
            firstName: other.firstName,
            lastName: other.lastName
          })

          return {
            chatId: chat.chatId,
            lastMessageId: chat.lastMessageId || null,
            lastMessageCiphertext: chat.lastMessageCiphertext || null,
            lastMessageTime: chat.lastMessageTime || null,
            title: `${other.firstName} ${other.lastName}`,
            partnerId: other.userId,
            partnerPublicId: other.publicId
          }
        }

        return {
          chatId: chat.chatId,
          lastMessageId: chat.lastMessageId || null,
          lastMessageCiphertext: chat.lastMessageCiphertext || null,
          lastMessageTime: chat.lastMessageTime || null,
          title: 'Chat'
        }
      })
      this.savePartners()
    },

    async createOrGetChat(publicId: string) {
      const userStore = useUserStore()

      // Получаем информацию о пользователе
      const user = await userStore.fetchUser(publicId)

      // Проверяем, есть ли уже чат с этим пользователем
      const existingChat = this.chats.find(c =>
        c.partnerId === user.id || c.partnerPublicId === user.publicId
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

      // Если чата нет, создаем новый

      try {
        // Используем правильное имя поля: userPublicId
        const res = await api.post('/chats', { userPublicId: publicId })

        const newChat = res.data
        const chatId = newChat.chatId || newChat.id

        if (!chatId) {
          console.error('No chatId in response:', res.data)
          throw new Error('Failed to create chat: no chatId returned')
        }

        // Сохраняем информацию о партнере
        this.chatPartners.set(chatId, {
          id: user.id,
          publicId: user.publicId,
          firstName: user.firstName,
          lastName: user.lastName
        })
        this.savePartners()

        const chatToAdd: Chat = {
          chatId: chatId,
          lastMessageId: newChat.lastMessageId || null,
          lastMessageCiphertext: newChat.lastMessageCiphertext || null,
          lastMessageTime: newChat.lastMessageTime || null,
          title: `${user.firstName} ${user.lastName}`,
          partnerId: user.id,
          partnerPublicId: user.publicId
        }

        this.chats.unshift(chatToAdd)

        return chatToAdd
      } catch (error) {
        console.error('Failed to create chat:', error)
        throw error
      }
    },

    async createChat(publicId: string) {
      return this.createOrGetChat(publicId)
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

    updateLastMessageFromServer(chatId: string, lastMessage: any) {
      const chat = this.chats.find(c => c.chatId === chatId)
      if (!chat) return

      if (lastMessage) {
        chat.lastMessageId = lastMessage.id
        chat.lastMessageCiphertext = lastMessage.ciphertext
        chat.lastMessageTime = lastMessage.createdDate
      } else {
        // Сообщений больше нет
        chat.lastMessageId = null
        chat.lastMessageCiphertext = null
        chat.lastMessageTime = null
      }

      console.log('Updated last message for chat:', chatId, chat)
    },

    updateLastMessage(chatId: string, message: any) {
      const chatIndex = this.chats.findIndex(c => c.chatId === chatId)

      if (chatIndex === -1) return

      const chat = this.chats[chatIndex]
      if (!chat) return

      chat.lastMessageId = message.id
      chat.lastMessageCiphertext = message.ciphertext
      chat.lastMessageTime = message.createdDate

      // Перемещаем чат вверх списка
      if (chatIndex > 0) {
        const reorderedChats = [...this.chats]
        const movedChat = reorderedChats.splice(chatIndex, 1)[0]
        if (movedChat) {
          reorderedChats.unshift(movedChat)
          this.chats = reorderedChats
        }
      }
    }
  }
})
