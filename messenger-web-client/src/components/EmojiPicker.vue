<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Picker } from 'emoji-picker-element'

const emit = defineEmits<{
  select: [emoji: string]
}>()

const pickerRef = ref<HTMLElement | null>(null)

onMounted(() => {
  if (pickerRef.value) {
    const picker = new Picker({
      locale: 'en',
      skinToneEmoji: '👍'
    })

    picker.addEventListener('emoji-click', (event: any) => {
      emit('select', event.detail.unicode)
    })

    pickerRef.value.appendChild(picker)
  }
})
</script>

<template>
  <div ref="pickerRef" class="emoji-picker"></div>
</template>

<style>
.emoji-picker {
  --background: #ffffff;
  --border-color: #e6e9ef;
  --border-size: 0px;
  --category-emoji-size: 1.2rem;
  --emoji-size: 1.5rem;
  --emoji-padding: 6px;
  --input-border-color: #e6e9ef;
  --input-padding: 8px 12px;
  --input-font-size: 14px;
  --num-columns: 8;
  --outline: none;
  --indicator-color: #9ca3af;
  --category-font-color: #6b7280;
  height: 420px;
  width: 352px;
  overflow: hidden;
}

.emoji-picker::part(content) {
  padding: 8px 4px 12px 4px;
}

.emoji-picker::part(emoji) {
  padding: 6px;
}

.emoji-picker::part(tab) {
  padding: 8px 0;
}

/* Увеличиваем отступ снизу для последнего ряда */
.emoji-picker::part(scroll) {
  padding-bottom: 8px;
  scrollbar-width: thin;
  scrollbar-color: #d1d5db transparent;
}

.emoji-picker::part(scroll)::-webkit-scrollbar {
  width: 4px;
}

.emoji-picker::part(scroll)::-webkit-scrollbar-track {
  background: transparent;
}

.emoji-picker::part(scroll)::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}
</style>
