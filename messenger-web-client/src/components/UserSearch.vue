<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import type { User } from '@/stores/user'

const userStore = useUserStore()
const chatStore = useChatStore()

const searchQuery = ref('')
const searchResults = ref<User[]>([])
const isSearching = ref(false)
const showResults = ref(false)

let searchTimeout: number | null = null

async function performSearch() {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }

  isSearching.value = true
  try {
    searchResults.value = await userStore.searchUsers(searchQuery.value)
    showResults.value = true
  } catch {
    // ignore
  } finally {
    isSearching.value = false
  }
}

function onSearchInput() {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }

  searchTimeout = window.setTimeout(() => {
    performSearch()
    searchTimeout = null
  }, 300)
}

async function startChat(user: User) {
  if (userStore.profile && user.publicId === userStore.profile.publicId) {
    return
  }

  try {
    const chat = await chatStore.createOrGetChat(user.publicId)
    chatStore.setChatPartner(chat.chatId, user)
    chatStore.setActiveChat(chat.chatId)

    searchQuery.value = ''
    searchResults.value = []
    showResults.value = false
  } catch {
    // ignore
  }
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.search-container')) {
    showResults.value = false
  }
}

function getUserInitials(user: User): string {
  return `${user.firstName?.charAt(0) || ''}${user.lastName?.charAt(0) || ''}`
}

function getUserDisplayName(user: User): string {
  return `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.publicId
}

function isCurrentUser(user: User): boolean {
  return user.publicId === userStore.profile?.publicId
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
})
</script>

<template>
  <div class="search-container">
    <div class="search-input-wrapper">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search by public ID"
        @input="onSearchInput"
        @focus="showResults = true"
      />
      <span v-if="isSearching" class="search-spinner">⟳</span>
    </div>

    <div
      v-if="showResults && (searchResults.length > 0 || searchQuery)"
      class="search-results"
    >
      <div v-if="isSearching" class="search-status">
        Searching...
      </div>

      <div v-else-if="searchResults.length === 0 && searchQuery" class="search-status">
        No users found
      </div>

      <div
        v-for="user in searchResults"
        :key="user.publicId"
        class="search-result-item"
        :class="{ 'is-self': isCurrentUser(user) }"
        @click="startChat(user)"
      >
        <div class="user-avatar">
          {{ getUserInitials(user) }}
        </div>
        <div class="user-info">
          <div class="user-name">
            {{ getUserDisplayName(user) }}
            <span v-if="isCurrentUser(user)" class="self-badge">(You)</span>
          </div>
          <div class="user-public-id">@{{ user.publicId }}</div>
        </div>
        <div v-if="isCurrentUser(user)" class="self-hint">
          💾
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-container {
  position: relative;
  z-index: 100;
}

.search-input-wrapper {
  padding: 12px;
  position: relative;
}

.search-input-wrapper input {
  width: 100%;
  height: 38px;
  border-radius: 20px;
  border: none;
  background: #f1f3f6;
  padding: 0 40px 0 14px;
  outline: none;
  box-sizing: border-box;
  font-size: 14px;
}

.search-input-wrapper input:focus {
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(58, 118, 240, 0.2);
}

.search-spinner {
  position: absolute;
  right: 22px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  font-size: 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: translateY(-50%) rotate(0deg); }
  to { transform: translateY(-50%) rotate(360deg); }
}

.search-results {
  position: absolute;
  top: 62px;
  left: 12px;
  right: 12px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  max-height: 400px;
  overflow-y: auto;
  z-index: 1000;
}

.search-status {
  padding: 16px;
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.search-result-item:hover {
  background: #f5f7fb;
}

.search-result-item.is-self {
  cursor: pointer;
  opacity: 0.8;
}

.search-result-item.is-self:hover {
  background: #f0f4ff;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.search-result-item.is-self .user-avatar {
  background: linear-gradient(135deg, #34d399 0%, #059669 100%);
}

.user-info {
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

.self-badge {
  color: #9ca3af;
  font-weight: normal;
  font-size: 12px;
  margin-left: 4px;
}

.user-public-id {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.self-hint {
  font-size: 16px;
  opacity: 0.6;
}
</style>
