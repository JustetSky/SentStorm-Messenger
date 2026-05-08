<script setup lang="ts">
import ChatList from '@/components/ChatList.vue'
import ChatView from '@/components/ChatView.vue'
import { useChatStore } from '@/stores/chat'
import { computed } from 'vue'

const chatStore = useChatStore()

const chatViewKey = computed(() => chatStore.activeChatId || 'no-chat')
</script>

<template>
  <div class="layout">
    <ChatList />
    <ChatView :key="chatViewKey" />
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
}
</style>
