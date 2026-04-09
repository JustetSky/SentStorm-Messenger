import { defineStore } from 'pinia'
import api from '@/api/api'

export interface Chat {
  chatId: string
  lastMessageId: string | null
  lastMessageCiphertext: string | null
  lastMessageTime: string | null
  title?: string
}

export const useChatStore = defineStore('chat', {
  state: () => ({
    chats: [] as Chat[],
    activeChatId: null as string | null
  }),

  getters: {
    activeChat: (state) => {
      return state.chats.find(c => c.chatId === state.activeChatId)
    }
  },

  actions: {
    async fetchChats() {
      const res = await api.get('/chats')
      this.chats = res.data.map((chat: Chat, index: number) => ({
        ...chat,
        title: `User ${index + 1}`
      }))
    },

    async createChat(publicId: string) {
      const res = await api.post('/chats', { publicId })
      const newChat = res.data

      // Добавляем новый чат в список
      this.chats.unshift({
        chatId: newChat.chatId,
        lastMessageId: null,
        lastMessageCiphertext: null,
        lastMessageTime: null,
        title: publicId
      })

      return newChat
    },

    setActiveChat(chatId: string) {
      this.activeChatId = chatId
    },

    updateLastMessage(chatId: string, message: any) {
      const chat = this.chats.find(c => c.chatId === chatId)
      if (chat) {
        chat.lastMessageId = message.id
        chat.lastMessageCiphertext = message.ciphertext
        chat.lastMessageTime = message.createdDate

        // Перемещаем чат вверх списка
        const index = this.chats.findIndex(c => c.chatId === chatId)
        if (index > 0) {
          const movedChat = this.chats[index]
          if (movedChat) {
            this.chats.splice(index, 1)
            this.chats.unshift(movedChat)
          }
        }
      }
    }
  }
})
