<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import keycloak from '@/auth/keycloak'
import api from '@/api/api'
import { cryptoService } from '@/services/crypto'

const userStore = useUserStore()
const showMenu = ref(false)
const currentStatus = ref('')

let statusInterval: number | null = null

// Функция отправки пинга для обновления lastSeen
async function pingOnline() {
  try {
    // Отправляем легкий запрос для обновления lastSeen на сервере
    await api.get('/users/me')
    await userStore.fetchMe() // Обновляем данные профиля
  } catch (error) {
    console.error('Failed to ping:', error)
  }
}

function toggleMenu() {
  showMenu.value = !showMenu.value
}

function openProfile() {
  showMenu.value = false
  emit('openProfile')
}

function logout() {
  keycloak.logout({ redirectUri: window.location.origin })
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.user-menu')) {
    showMenu.value = false
  }
}

const isOnline = computed(() => {
  // Всегда показываем себя как онлайн
  return true
})

function formatLastSeen(lastSeen: string | undefined): string {
  // Для себя всегда показываем "Online"
  return 'Online'
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)

  // Пингуем сразу
  pingOnline()

  // Пингуем каждые 30 секунд для поддержания онлайн статуса
  statusInterval = window.setInterval(pingOnline, 30000)

  // Пингуем при возвращении на вкладку
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      pingOnline()
    }
  })
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (statusInterval) {
    clearInterval(statusInterval)
  }
})

const emit = defineEmits<{
  openProfile: []
}>()
</script>

<template>
  <div class="user-menu">
    <button class="burger-btn" @click.stop="toggleMenu">
      <span></span>
      <span></span>
      <span></span>
    </button>

    <div v-if="showMenu" class="menu-dropdown" @click.stop>
      <div class="menu-header">
        <div class="user-info">
          <div class="user-avatar online">
            {{ userStore.profile?.firstName?.charAt(0) }}{{ userStore.profile?.lastName?.charAt(0) }}
            <span class="online-indicator"></span>
          </div>
          <div class="user-details">
            <div class="user-name">{{ userStore.profile?.firstName }} {{ userStore.profile?.lastName }}</div>
            <div class="user-status online-text">Online</div>
            <div class="user-public-id">@{{ userStore.profile?.publicId }}</div>
          </div>
        </div>
      </div>
      <div class="menu-items">
        <div class="menu-item" @click="openProfile">
          <span>Profile</span>
        </div>
        <div class="menu-item logout" @click="logout">
          <span>Logout</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-menu {
  position: relative;
}

.burger-btn {
  width: 40px;
  height: 40px;
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.burger-btn:hover {
  background: #f1f3f6;
}

.burger-btn span {
  display: block;
  width: 20px;
  height: 2px;
  background: #6b7280;
  border-radius: 1px;
  transition: all 0.2s;
}

.burger-btn:hover span {
  background: #111827;
}

.menu-dropdown {
  position: absolute;
  top: 48px;
  left: 0;
  width: 280px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  overflow: hidden;
}

.menu-header {
  padding: 16px;
  border-bottom: 1px solid #e6e9ef;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #22c55e;
  border: 2px solid white;
  border-radius: 50%;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.user-public-id {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.menu-items {
  padding: 8px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s;
  font-size: 14px;
  color: #111827;
}

.menu-item:hover {
  background: #f5f7fb;
}

.menu-item.logout {
  color: #ef4444;
}

.online-text {
  color: #22c55e !important;
}
</style>
