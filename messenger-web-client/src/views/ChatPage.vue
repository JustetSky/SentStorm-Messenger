<script setup lang="ts">
import ChatList from '@/components/ChatList.vue'
import ChatView from '@/components/ChatView.vue'
</script>

<template>
  <div class="layout">
    <ChatList />
    <ChatView />
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
