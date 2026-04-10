<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import type { User } from '@/stores/user'
import { computed } from 'vue'

const props = defineProps<{
  show: boolean
  user?: User | null
}>()

const emit = defineEmits<{
  close: []
}>()

const userStore = useUserStore()

const isOnline = computed(() => {
  const targetUser = props.user || userStore.profile
  if (!targetUser?.lastSeen) return false
  const lastSeen = new Date(targetUser.lastSeen)
  const now = new Date()
  const diffSeconds = (now.getTime() - lastSeen.getTime()) / 1000
  return diffSeconds < 60
})

function close() {
  emit('close')
}

function formatLastSeen(lastSeen: string | undefined): string {
  if (!lastSeen) return 'Never'

  const date = new Date(lastSeen)
  const now = new Date()
  const diffSeconds = (now.getTime() - date.getTime()) / 1000

  // Для себя всегда показываем Online
  if (!props.user) return 'Online'

  // Для других пользователей
  if (diffSeconds < 60) return 'Online'
  if (diffSeconds < 3600) return `Last seen ${Math.floor(diffSeconds / 60)} min ago`
  if (diffSeconds < 86400) return `Last seen today at ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  if (diffSeconds < 172800) return `Last seen yesterday at ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  return `Last seen ${date.toLocaleDateString()}`
}
</script>

<template>
  <div v-if="show" class="profile-modal" @click="close">
    <div class="profile-content" @click.stop>
      <div class="profile-header">
        <h3>{{ user ? 'Profile' : 'My Profile' }}</h3>
        <button class="close-btn" @click="close">×</button>
      </div>

      <div v-if="user || userStore.profile" class="profile-body">
        <div class="profile-avatar" :class="{ online: isOnline }">
          {{ (user || userStore.profile)?.firstName?.charAt(0) }}{{ (user || userStore.profile)?.lastName?.charAt(0) }}
          <span v-if="isOnline" class="online-indicator"></span>
        </div>

        <div class="profile-name">
          {{ (user || userStore.profile)?.firstName }} {{ (user || userStore.profile)?.lastName }}
        </div>

        <div class="profile-field">
          <label>Public ID</label>
          <div class="field-value">@{{ (user || userStore.profile)?.publicId }}</div>
        </div>

        <div class="profile-field">
          <label>Email</label>
          <div class="field-value">{{ (user || userStore.profile)?.email }}</div>
        </div>

        <div class="profile-field">
          <label>Status</label>
          <div class="field-value" :class="{ online: isOnline }">
            {{ formatLastSeen((user || userStore.profile)?.lastSeen) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.profile-content {
  background: white;
  border-radius: 16px;
  width: 420px;
  max-width: 90%;
  max-height: 80vh;
  overflow: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e6e9ef;
}

.profile-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #6b7280;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: background 0.15s;
}

.close-btn:hover {
  background: #f1f3f6;
  color: #111827;
}

.profile-body {
  padding: 24px;
}

.profile-avatar {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 28px;
  margin: 0 auto 16px;
}

.online-indicator {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 16px;
  height: 16px;
  background: #22c55e;
  border: 3px solid white;
  border-radius: 50%;
}

.profile-name {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 24px;
}

.profile-field {
  margin-bottom: 16px;
}

.profile-field label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.field-value {
  font-size: 15px;
  color: #111827;
  padding: 8px 0;
}

.field-value.online {
  color: #22c55e;
  font-weight: 500;
}
</style>
