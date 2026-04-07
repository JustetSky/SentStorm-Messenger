import { defineStore } from 'pinia'
import api from '@/api/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: null as null | {
      id: string,
      publicId: string
      email: string
      firstName: string
      lastName: string
      lastSeen: string
    }
  }),

  actions: {
    async fetchMe() {
      const res = await api.get('/users/me')
      this.profile = res.data
    }
  }
})
