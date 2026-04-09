<script setup lang="ts">
import { useMessageStore } from '@/stores/message'
import { useUserStore } from '@/stores/user'
import { watch, nextTick } from 'vue'

const messageStore = useMessageStore()
const userStore = useUserStore()

watch(
  () => messageStore.messages.length,
  async () => {
    await nextTick()
    scrollToBottomIfNear()
  }
)

function scrollToBottomIfNear() {
  const container = document.querySelector('.messages')
  if (!container) return

  const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100
  if (isNearBottom) {
    container.scrollTop = container.scrollHeight
  }
}

function getStatusIcon(state: string) {
  switch (state) {
    case 'SENDING':
      return '🕒'
    case 'SENT':
      return '✓'
    case 'DELIVERED':
      return '✓✓'
    case 'READ':
      return '✓✓'
    case 'ERROR':
      return '⚠️'
    default:
      return ''
  }
}

function getStatusColor(state: string) {
  switch (state) {
    case 'READ':
      return '#3a76f0'
    case 'SENDING':
      return '#9ca3af'
    default:
      return '#9ca3af'
  }
}

function formatTime(dateString: string) {
  const date = new Date(dateString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="messages-list">
    <div class="messages-inner">
      <div
        v-for="msg in messageStore.messages"
        :key="msg.id"
        :data-message-id="msg.id"
        class="message-row"
        :class="{ mine: msg.senderId === userStore.profile?.id }"
      >
        <div class="bubble-wrapper">
          <div class="bubble">
            <div class="message-text">{{ msg.ciphertext }}</div>
          </div>
          <div class="message-footer">
            <span class="time">{{ formatTime(msg.createdDate) }}</span>
            <span
              v-if="msg.senderId === userStore.profile?.id"
              class="status"
              :style="{ color: getStatusColor(msg.state) }"
            >
              {{ getStatusIcon(msg.state) }}
            </span>
          </div>
        </div>
      </div>
      <!-- Добавляем пустой элемент для отступа снизу -->
      <div class="bottom-spacer"></div>
    </div>
  </div>
</template>

<style scoped>
.messages-list {
  display: flex;
  justify-content: center;
}

.messages-inner {
  width: 100%;
  max-width: 800px;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bottom-spacer {
  height: 16px;
  flex-shrink: 0;
}

.message-row {
  display: flex;
  width: 100%;
}

.message-row:not(.mine) {
  justify-content: flex-start;
  padding-right: 80px;
}

.message-row.mine {
  justify-content: flex-end;
  padding-left: 80px;
}

.bubble-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 520px;
}

.message-row.mine .bubble-wrapper {
  align-items: flex-end;
}

.message-row:not(.mine) .bubble-wrapper {
  align-items: flex-start;
}

.bubble {
  padding: 10px 14px;
  border-radius: 16px;
  background: #e5e7eb;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
  width: fit-content;
}

.message-row.mine .bubble {
  background: #3a76f0;
  color: white;
}

.message-text {
  word-break: break-word;
}

.message-footer {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding: 0 4px;
}

.time {
  font-size: 11px;
  color: #6b7280;
  line-height: 1;
}

.status {
  font-size: 12px;
  line-height: 1;
  font-weight: bold;
}
</style>
