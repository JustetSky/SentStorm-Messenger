import { defineStore } from 'pinia'
import api from '@/api/api'

export interface User {
  id: string
  publicId: string
  email?: string
  firstName: string
  lastName: string
  lastSeen?: string
}

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: null as User | null,
    userCache: new Map<string, User>()
  }),

  actions: {
    async fetchMe() {
      const res = await api.get('/users/me')
      this.profile = res.data
      if (this.profile) {
        this.userCache.set(this.profile.id, this.profile)
        this.userCache.set(this.profile.publicId, this.profile)
      }
    },

    async fetchUser(publicId: string): Promise<User> {
      const cached = this.userCache.get(publicId)
      if (cached) return cached

      const res = await api.get(`/users/${publicId}`)
      const user = res.data
      this.userCache.set(user.id, user)
      this.userCache.set(user.publicId, user)
      return user
    },

    async searchUsers(query: string): Promise<User[]> {
      if (!query.trim()) return []

      const res = await api.get('/users/search', {
        params: { query: query }
      })

      let users = []
      if (Array.isArray(res.data)) {
        users = res.data
      } else if (res.data.items) {
        users = res.data.items
      } else if (res.data.content) {
        users = res.data.content
      } else {
        users = []
      }

      users.forEach((user: User) => {
        const userId = user.id || user.publicId
        this.userCache.set(userId, user)
        this.userCache.set(user.publicId, user)
      })

      return users
    },

    getUserById(id: string): User | undefined {
      return this.userCache.get(id)
    }
  }
})
