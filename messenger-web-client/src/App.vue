<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

const userStore = useUserStore()
const chatStore = useChatStore()

onMounted(async () => {
  await userStore.fetchMe()
  await chatStore.fetchChats()
})
</script>

<template>
  <router-view />
</template>
