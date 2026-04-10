<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import keycloak from '@/auth/keycloak'

const userStore = useUserStore()
const showMenu = ref(false)

function toggleMenu() {
  showMenu.value = !showMenu.value
}

function closeMenu() {
  showMenu.value = false
}

function openProfile() {
  showMenu.value = false
  emit('openProfile')
}

function logout() {
  keycloak.logout({
    redirectUri: window.location.origin
  })
}

// Закрываем меню при клике вне
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.user-menu')) {
    showMenu.value = false
  }
}

// Слушаем клики вне компонента
import { onMounted, onUnmounted } from 'vue'
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
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
          <div class="user-avatar">
            {{ userStore.profile?.firstName?.charAt(0) }}{{ userStore.profile?.lastName?.charAt(0) }}
          </div>
          <div class="user-details">
            <div class="user-name">{{ userStore.profile?.firstName }} {{ userStore.profile?.lastName }}</div>
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
  background: #111827;
  border-radius: 1px;
  transition: all 0.2s;
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

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-public-id {
  font-size: 13px;
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

.menu-item .icon {
  font-size: 18px;
  width: 20px;
  text-align: center;
}
</style>
