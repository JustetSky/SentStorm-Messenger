<script setup lang="ts">
import { ref } from 'vue'
import { useChatStore } from '@/stores/chat'
import UserSearch from './UserSearch.vue'
import UserMenu from './UserMenu.vue'
import UserProfile from './UserProfile.vue'
import ContextMenu from './ContextMenu.vue'
import type { MenuItem } from './ContextMenu.vue'
import api from '@/api/api'

const chatStore = useChatStore()
const showProfile = ref(false)

const contextMenuRef = ref<InstanceType<typeof ContextMenu> | null>(null)
const selectedChatId = ref<string | null>(null)

function openChat(chatId: string) {
  chatStore.setActiveChat(chatId)
}

function onContextMenu(event: MouseEvent, chatId: string) {
  event.preventDefault()
  event.stopPropagation()
  selectedChatId.value = chatId
  contextMenuRef.value?.show(event.clientX, event.clientY)
}

function closeContextMenu() {
  selectedChatId.value = null
}

async function deleteChat() {
  if (!selectedChatId.value) return

  if (!confirm('Are you sure you want to delete this chat?')) {
    closeContextMenu()
    return
  }

  const chatId = selectedChatId.value

  try {
    await api.delete(`/chats/${chatId}`)
    chatStore.deleteChat(chatId)
    console.log('Chat deleted successfully:', chatId)
  } catch (error) {
    console.error('Failed to delete chat:', error)
    alert('Failed to delete chat')
  } finally {
    closeContextMenu()
  }
}

const menuItems: MenuItem[] = [
  {
    label: 'Delete chat',
    action: deleteChat,
    danger: true
  }
]

function formatTime(dateString: string | null) {
  if (!dateString) return ''

  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return 'Yesterday'
  } else {
    return date.toLocaleDateString()
  }
}

function getChatTitle(chatId: string): string {
  return chatStore.getChatTitle(chatId)
}
</script>

<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <UserMenu @open-profile="showProfile = true" />
      <UserSearch />
    </div>

    <div class="chat-list">
      <div
        v-for="chat in chatStore.chats"
        :key="chat.chatId"
        class="chat-item"
        :class="{ active: chat.chatId === chatStore.activeChatId }"
        @click="openChat(chat.chatId)"
        @contextmenu="onContextMenu($event, chat.chatId)"
      >
        <div class="chat-avatar">
          {{ getChatTitle(chat.chatId).split(' ').map(w => w[0]).join('').slice(0, 2) }}
        </div>
        <div class="chat-content">
          <div class="top-row">
            <span class="chat-name">
              {{ getChatTitle(chat.chatId) }}
            </span>

            <span class="time">
              {{ formatTime(chat.lastMessageTime) }}
            </span>
          </div>

          <div class="bottom-row">
            {{ chat.lastMessageCiphertext || (chat.lastMessageId ? 'Message' : 'No messages yet') }}
          </div>
        </div>
      </div>
    </div>

    <UserProfile :show="showProfile" @close="showProfile = false" />

    <ContextMenu
      ref="contextMenuRef"
      :items="menuItems"
      @close="closeContextMenu"
    />
  </div>
</template>

<style scoped>
.sidebar {
  width: 350px;
  min-width: 350px;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e6e9ef;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  padding: 8px 8px 8px 4px;
  border-bottom: 1px solid #e6e9ef;
}

.sidebar-header :deep(.search-container) {
  flex: 1;
}

.sidebar-header :deep(.search-input-wrapper) {
  padding: 0;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.chat-item:hover {
  background: #f5f7fb;
}

.chat-item.active {
  background: #e9f2ff;
}

.chat-avatar {
  width: 48px;
  height: 48px;
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

.chat-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.chat-name {
  font-weight: 600;
  font-size: 14px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.time {
  font-size: 12px;
  color: #9ca3af;
  flex-shrink: 0;
}

.bottom-row {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
