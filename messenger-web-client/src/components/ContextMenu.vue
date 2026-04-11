<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

export interface MenuItem {
  label: string
  action: () => void
  danger?: boolean
}

const props = defineProps<{
  items: MenuItem[]
}>()

const emit = defineEmits<{
  close: []
}>()

const menuRef = ref<HTMLElement | null>(null)
const position = ref({ x: 0, y: 0 })
const visible = ref(false)

function show(x: number, y: number) {
  // Сначала скрываем меню
  visible.value = false

  // Ждем следующий тик для сброса позиции
  nextTick(() => {
    // Вычисляем позицию с учетом границ экрана
    const menuWidth = 150
    const menuHeight = 40

    let posX = x
    let posY = y

    // Не выходим за правый край
    if (posX + menuWidth > window.innerWidth) {
      posX = window.innerWidth - menuWidth - 10
    }

    // Не выходим за нижний край
    if (posY + menuHeight > window.innerHeight) {
      posY = window.innerHeight - menuHeight - 10
    }

    position.value = { x: posX, y: posY }
    visible.value = true
  })
}

function hide() {
  visible.value = false
  emit('close')
}

function handleClickOutside(event: MouseEvent) {
  if (visible.value && menuRef.value && !menuRef.value.contains(event.target as Node)) {
    hide()
  }
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && visible.value) {
    hide()
  }
}

function handleScroll() {
  if (visible.value) {
    hide()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEscape)
  document.addEventListener('scroll', handleScroll, true)
  window.addEventListener('resize', hide)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscape)
  document.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('resize', hide)
})

defineExpose({ show, hide })
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="menuRef"
      class="context-menu"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
      @click.stop
      @contextmenu.prevent
    >
      <div
        v-for="(item, index) in items"
        :key="index"
        class="context-menu-item"
        :class="{ danger: item.danger }"
        @click="item.action(); hide()"
      >
        {{ item.label }}
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.context-menu {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 4px;
  min-width: 150px;
  z-index: 9999;
}

.context-menu-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #111827;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
  white-space: nowrap;
}

.context-menu-item:hover {
  background: #f5f7fb;
}

.context-menu-item.danger {
  color: #ef4444;
}

.context-menu-item.danger:hover {
  background: #fef2f2;
}
</style>
