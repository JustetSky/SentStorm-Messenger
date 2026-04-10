import { defineStore } from 'pinia'
import api from '@/api/api'
import { useUserStore } from './user'

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
    // Загружаем сохраненные данные о партнерах из localStorage
    loadSavedPartners() {
      try {
        const saved = localStorage.getItem('chatPartners')
        if (saved) {
          const partners = JSON.parse(saved)
          this.chatPartners = new Map(Object.entries(partners))
        }
      } catch (e) {
        console.error('Failed to load saved partners:', e)
      }
    },

    // Сохраняем данные о партнерах в localStorage
    savePartners() {
      try {
        const obj = Object.fromEntries(this.chatPartners)
        localStorage.setItem('chatPartners', JSON.stringify(obj))
      } catch (e) {
        console.error('Failed to save partners:', e)
      }
    },

    async fetchChats() {
      this.loadSavedPartners()

      const userStore = useUserStore()
      const res = await api.get('/chats')

      console.log('Fetched chats:', res.data)

      const chats: Chat[] = []

      for (const chat of res.data) {
        // Проверяем, есть ли сохраненная информация о партнере
        const savedPartner = this.chatPartners.get(chat.chatId)

        if (savedPartner) {
          // Используем сохраненные данные
          chats.push({
            chatId: chat.chatId,
            lastMessageId: chat.lastMessageId || null,
            lastMessageCiphertext: chat.lastMessageCiphertext || null,
            lastMessageTime: chat.lastMessageTime || null,
            title: `${savedPartner.firstName} ${savedPartner.lastName}`,
            partnerId: savedPartner.id,
            partnerPublicId: savedPartner.publicId
          })
          continue
        }

        // Если нет сохраненных данных, пытаемся получить с сервера
        try {
          const chatInfo = await api.get(`/chats/${chat.chatId}`)
          console.log('Chat info:', chatInfo.data)

          const participants = chatInfo.data.participants || chatInfo.data.members || []

          const partner = participants.find((p: any) => {
            const partnerId = p.userId || p.id
            return partnerId !== userStore.profile?.id
          })

          if (partner) {
            const partnerPublicId = partner.publicId || partner.userPublicId

            if (partnerPublicId) {
              try {
                const user = await userStore.fetchUser(partnerPublicId)

                // Сохраняем информацию о партнере
                this.chatPartners.set(chat.chatId, {
                  id: user.id,
                  publicId: user.publicId,
                  firstName: user.firstName,
                  lastName: user.lastName
                })
                this.savePartners()

                chats.push({
                  chatId: chat.chatId,
                  lastMessageId: chat.lastMessageId || null,
                  lastMessageCiphertext: chat.lastMessageCiphertext || null,
                  lastMessageTime: chat.lastMessageTime || null,
                  title: `${user.firstName} ${user.lastName}`,
                  partnerId: user.id,
                  partnerPublicId: user.publicId
                })
              } catch (e) {
                chats.push({
                  chatId: chat.chatId,
                  lastMessageId: chat.lastMessageId || null,
                  lastMessageCiphertext: chat.lastMessageCiphertext || null,
                  lastMessageTime: chat.lastMessageTime || null,
                  title: partnerPublicId,
                  partnerPublicId: partnerPublicId
                })
              }
            } else {
              chats.push({
                ...chat,
                lastMessageId: chat.lastMessageId || null,
                lastMessageCiphertext: chat.lastMessageCiphertext || null,
                lastMessageTime: chat.lastMessageTime || null,
              })
            }
          } else {
            chats.push({
              ...chat,
              lastMessageId: chat.lastMessageId || null,
              lastMessageCiphertext: chat.lastMessageCiphertext || null,
              lastMessageTime: chat.lastMessageTime || null,
            })
          }
        } catch (error) {
          console.error('Failed to fetch chat info for', chat.chatId, error)
          chats.push({
            chatId: chat.chatId,
            lastMessageId: chat.lastMessageId || null,
            lastMessageCiphertext: chat.lastMessageCiphertext || null,
            lastMessageTime: chat.lastMessageTime || null,
            title: 'Chat'
          })
        }
      }

      this.chats = chats
      console.log('Processed chats:', this.chats)
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
        console.log('Chat already exists:', existingChat.chatId)

        // Обновляем информацию о партнере на всякий случай
        this.chatPartners.set(existingChat.chatId, {
          id: user.id,
          publicId: user.publicId,
          firstName: user.firstName,
          lastName: user.lastName
        })
        this.savePartners()

        return existingChat
      }

      // Если чата нет, создаем новый
      console.log('Creating new chat with:', publicId)
      const res = await api.post('/chats', { publicId })
      const newChat = res.data

      // Сохраняем информацию о партнере
      this.chatPartners.set(newChat.chatId, {
        id: user.id,
        publicId: user.publicId,
        firstName: user.firstName,
        lastName: user.lastName
      })
      this.savePartners()

      const chatToAdd: Chat = {
        chatId: newChat.chatId,
        lastMessageId: null,
        lastMessageCiphertext: null,
        lastMessageTime: null,
        title: `${user.firstName} ${user.lastName}`,
        partnerId: user.id,
        partnerPublicId: user.publicId
      }

      this.chats.unshift(chatToAdd)

      return newChat
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

    setActiveChat(chatId: string) {
      this.activeChatId = chatId
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
