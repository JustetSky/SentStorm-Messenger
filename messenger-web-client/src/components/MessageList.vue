<script setup lang="ts">
import { useMessageStore } from '@/stores/message'
import { useUserStore } from '@/stores/user'

const messageStore = useMessageStore()
const userStore = useUserStore()
</script>

<template>
  <div class="messages">
    <div class="messages-inner">
      <div
        v-for="msg in messageStore.messages"
        :key="msg.id"
        class="message-row"
        :class="{ mine: msg.senderId === userStore.profile?.id }"
      >
        <div class="bubble">
          {{ msg.ciphertext }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.messages {
  flex: 1;
  overflow-y: auto;

  display: flex;
  justify-content: center;

  padding: 12px 0 12px;
}

/* контейнер сообщений */
.messages-inner {
  width: 100%;
  max-width: 800px;

  padding: 0 16px;

  display: flex;
  flex-direction: column;
  gap: 10px;
}

.messages-inner::before {
  content: '';
  height: 10px;
  flex-shrink: 0;
}

/* СТРОКА СООБЩЕНИЯ */
.message-row {
  display: flex;
  width: 100%;
}

/* ЧУЖИЕ — слева с отступом */
.message-row:not(.mine) {
  justify-content: flex-start;
  padding-right: 80px;
}

/* МОИ — справа с отступом */
.message-row.mine {
  justify-content: flex-end;
  padding-left: 80px;
}

.bubble {
  max-width: 520px;

  padding: 10px 14px;
  border-radius: 16px;

  background: #e5e7eb;
  font-size: 14px;
  line-height: 1.4;

  word-break: break-word;
}

.message-row.mine .bubble {
  background: #3a76f0;
  color: white;
}
</style>
