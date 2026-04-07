<script setup lang="ts">
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
</script>

<template>
  <div class="chat-container">

    <div v-if="chatStore.activeChat" class="chat">

      <!-- HEADER -->
      <div class="header">
        {{ chatStore.activeChat.title }}
      </div>

      <!-- MESSAGES -->
      <div class="messages">
        <div class="empty">No messages yet</div>
      </div>

      <!-- INPUT -->
      <div class="input-wrapper">
        <div class="input">
          <input placeholder="Message" />
          <button>➤</button>
        </div>
      </div>

    </div>

    <div v-else class="no-chat">
      Select a chat
    </div>

  </div>
</template>

<style scoped>
.chat-container {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
}

.chat {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

/* HEADER */
.header {
  height: 60px;
  background: white;
  border-bottom: 1px solid #e6e9ef;
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-weight: 600;
  flex-shrink: 0;
}

/* MESSAGES */
.messages {
  flex: 1;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
}

/* INPUT */
.input-wrapper {
  display: flex;
  justify-content: center;
  padding: 12px;
  background: white;
  border-top: 1px solid #e6e9ef;
  flex-shrink: 0;
}

.input {
  width: 100%;
  max-width: 800px;
  display: flex;
  gap: 10px;
}

.input input {
  flex: 1;
  height: 42px;
  border-radius: 20px;
  border: none;
  background: #f1f3f6;
  padding: 0 16px;
  outline: none;
}

.input button {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: #3a76f0;
  color: white;
  cursor: pointer;
}

.no-chat {
  margin: auto;
  color: #9ca3af;
}

.empty {
  color: #9ca3af;
}
</style>
