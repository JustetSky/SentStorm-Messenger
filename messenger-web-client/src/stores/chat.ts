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
    activeChat(state) {
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

    setActiveChat(chatId: string) {
      this.activeChatId = chatId
    }
  }
})
