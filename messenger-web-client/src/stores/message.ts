import { defineStore } from 'pinia'
import api from '@/api/api'

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
    loading: false
  }),

  actions: {
    async fetchMessages(chatId: string) {
      this.loading = true

      try {
        const res = await api.get(`/chats/${chatId}/messages?page=0&size=20`)
        this.messages = res.data.items.reverse()
      } finally {
        this.loading = false
      }
    },

    clear() {
      this.messages = []
    }
  }
})
