import { defineStore } from 'pinia'
import { ref } from 'vue'
import { sendChatMessage } from '@/api/dashboard'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])
  const isOpen = ref(false)
  const input = ref('')
  const loading = ref(false)
  const error = ref('')

  function toggle() {
    isOpen.value = !isOpen.value
  }

  async function send() {
    const msg = input.value.trim()
    if (!msg || loading.value) return
    input.value = ''
    error.value = ''
    messages.value.push({ role: 'user', content: msg })
    loading.value = true
    try {
      const res = await sendChatMessage(msg)
      messages.value.push({ role: 'ai', content: res.answer ?? '응답을 받았습니다.' })
    } catch {
      error.value = '잠시 후 다시 시도해주세요.'
      messages.value.push({ role: 'ai', content: '오류가 발생했습니다. 잠시 후 다시 시도해주세요.' })
    } finally {
      loading.value = false
    }
  }

  function clear() {
    messages.value = []
  }

  return { messages, isOpen, input, loading, error, toggle, send, clear }
})
