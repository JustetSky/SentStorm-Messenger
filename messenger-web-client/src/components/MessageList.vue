<script setup lang="ts">
import { useMessageStore } from '@/stores/message'
import { useUserStore } from '@/stores/user'
import { watch, nextTick, ref } from 'vue'
import ContextMenu from './ContextMenu.vue'
import type { MenuItem } from './ContextMenu.vue'
import api from '@/api/api'
import keycloak from '@/auth/keycloak'

const messageStore = useMessageStore()
const userStore = useUserStore()

const contextMenuRef = ref<InstanceType<typeof ContextMenu> | null>(null)
const selectedMessageId = ref<string | null>(null)
const showImageViewer = ref(false)
const currentImage = ref('')

function onBubbleContextMenu(event: MouseEvent, messageId: string) {
  const message = messageStore.messages.find(m => m.id === messageId)

  // Можно удалять только свои сообщения
  if (message?.senderId !== userStore.profile?.id) {
    return
  }

  event.preventDefault()
  event.stopPropagation()
  selectedMessageId.value = messageId
  contextMenuRef.value?.show(event.clientX, event.clientY)
}

function closeContextMenu() {
  selectedMessageId.value = null
}

async function deleteMessage() {
  if (!selectedMessageId.value) return

  const messageId = selectedMessageId.value

  try {
    await api.delete(`/messages/${messageId}`)
    messageStore.deleteMessage(messageId)
    console.log('Message deleted successfully:', messageId)
  } catch (error) {
    console.error('Failed to delete message:', error)
    alert('Failed to delete message')
  } finally {
    closeContextMenu()
  }
}

const menuItems: MenuItem[] = [
  {
    label: 'Delete message',
    action: deleteMessage,
    danger: true
  }
]

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
    case 'SENDING': return '🕒'
    case 'SENT': return '✓'
    case 'DELIVERED': return '✓✓'
    case 'READ': return '✓✓'
    case 'ERROR': return '⚠️'
    default: return ''
  }
}

function getStatusColor(state: string) {
  switch (state) {
    case 'READ': return '#3a76f0'
    case 'SENDING': return '#9ca3af'
    default: return '#9ca3af'
  }
}

function formatTime(dateString: string) {
  const date = new Date(dateString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Проверяем, является ли сообщение изображением
function isImageMessage(msg: any): boolean {
  // Проверяем по типу сообщения
  if (msg.type === 'IMAGE') return true

  // Проверяем по содержимому
  try {
    const parsed = JSON.parse(msg.ciphertext)
    return parsed.type === 'IMAGE'
  } catch {
    return false
  }
}

// Извлекаем URL изображения из сообщения
function getImageUrl(msg: any): string | null {
  try {
    const parsed = JSON.parse(msg.ciphertext)
    if (parsed.type === 'IMAGE' && parsed.url) {
      const url = parsed.url.startsWith('/')
        ? `https://localhost:8443${parsed.url}`
        : parsed.url
      // Берём токен из Keycloak
      const token = keycloak.token
      return token ? `${url}?token=${token}` : url
    }
  } catch {
    // ignore
  }
  return null
}

// Извлекаем имя файла
function getImageName(msg: any): string {
  try {
    const parsed = JSON.parse(msg.ciphertext)
    return parsed.name || 'Image'
  } catch {
    return 'Image'
  }
}

// Открыть изображение в модальном окне
function openImage(url: string | null) {
  if (url) {
    currentImage.value = url
    showImageViewer.value = true
  }
}

function closeImageViewer() {
  showImageViewer.value = false
  currentImage.value = ''  // Очищаем пустой строкой
}

// Получить текст для отображения (для текстовых сообщений)
function getDisplayText(msg: any): string {
  if (isImageMessage(msg)) {
    return '' // Не показываем текст для изображений
  }
  return msg.ciphertext
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
          <div
            class="bubble"
            :class="{
              'own-bubble': msg.senderId === userStore.profile?.id,
              'image-bubble': isImageMessage(msg)
            }"
            @contextmenu="onBubbleContextMenu($event, msg.id)"
          >
            <!-- Изображение -->
            <template v-if="isImageMessage(msg)">
              <img
                :src="getImageUrl(msg) || ''"
                :alt="getImageName(msg)"
                class="message-image"
                @click="openImage(getImageUrl(msg))"
              />
            </template>
            <!-- Текст -->
            <template v-else>
              <div class="message-text">{{ getDisplayText(msg) }}</div>
            </template>
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
      <div class="bottom-spacer"></div>
    </div>

    <ContextMenu
      ref="contextMenuRef"
      :items="menuItems"
      @close="closeContextMenu"
    />

    <!-- Модальное окно для просмотра изображений -->
    <Teleport to="body">
      <div v-if="showImageViewer && currentImage" class="image-viewer" @click="closeImageViewer">
        <img :src="currentImage" @click.stop alt="Full size" />
        <button class="close-viewer-btn" @click="closeImageViewer">×</button>
      </div>
    </Teleport>
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
  cursor: default;
  user-select: text;
}

.bubble.image-bubble {
  padding: 4px !important;
  background: transparent !important;
}

.bubble.own-bubble {
  cursor: context-menu;
}

.message-row.mine .bubble {
  background: #3a76f0;
  color: white;
}

.message-row.mine .bubble.image-bubble {
  background: transparent !important;
}

.message-text {
  word-break: break-word;
}

.message-image {
  max-width: 300px;
  max-height: 300px;
  border-radius: 12px;
  cursor: pointer;
  object-fit: cover;
  display: block;
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

/* Модальное окно для просмотра изображений */
.image-viewer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  cursor: zoom-out;
}

.image-viewer img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  cursor: default;
}

.close-viewer-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-size: 28px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}

.close-viewer-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
