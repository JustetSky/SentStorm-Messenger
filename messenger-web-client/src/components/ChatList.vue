<script setup lang="ts">
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()

function openChat(chatId: string) {
  chatStore.setActiveChat(chatId)
}
</script>

<template>
  <div class="sidebar">
    <!-- SEARCH -->
    <div class="search">
      <input placeholder="Search" />
    </div>

    <!-- LIST -->
    <div class="chat-list">
      <div
        v-for="chat in chatStore.chats"
        :key="chat.chatId"
        class="chat-item"
        :class="{ active: chat.chatId === chatStore.activeChatId }"
        @click="openChat(chat.chatId)"
      >
        <div class="chat-content">
          <div class="top-row">
            <span class="chat-name">
              {{ chat.title }}
            </span>

            <span class="time">
              {{ chat.lastMessageTime ? '12:30' : '' }}
            </span>
          </div>

          <div class="bottom-row">
            {{ chat.lastMessageCiphertext || 'No messages yet' }}
          </div>
        </div>
      </div>
    </div>
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

.search {
  padding: 12px;
}

.search input {
  width: 100%;
  height: 38px;
  border-radius: 20px;
  border: none;
  background: #f1f3f6;
  padding: 0 14px;
  outline: none;
  box-sizing: border-box;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}

.chat-item {
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

.chat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.top-row {
  display: flex;
  justify-content: space-between;
}

.chat-name {
  font-weight: 600;
  font-size: 14px;
  color: #111827;
}

.time {
  font-size: 12px;
  color: #9ca3af;
}

.bottom-row {
  font-size: 13px;
  color: #6b7280;
}
</style>
