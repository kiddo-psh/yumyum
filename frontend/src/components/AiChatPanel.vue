<template>
  <Transition name="chat-panel">
    <div
      v-if="chat.isOpen"
      class="fixed bottom-36 right-10 z-50 flex flex-col bg-surface neo-brutal-border rounded-xl overflow-hidden neo-brutal-shadow"
      style="width: 560px; height: 700px; max-height: calc(100vh - 10rem);"
    >
      <!-- 헤더 -->
      <div class="flex items-center justify-between px-6 py-5 border-b-[3px] border-on-background flex-shrink-0">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-nyam-mint neo-brutal-border rounded-xl flex items-center justify-center flex-shrink-0 overflow-hidden">
            <img src="/nyam/nyam_logo.png" alt="냠냠코치" class="w-9 h-9 object-contain" />
          </div>
          <div>
            <p class="text-headline-md font-bold text-on-background leading-none mb-1">AI 영양 상담</p>
            <p class="text-label-lg text-on-surface-variant">냠냠 코치에게 무엇이든 물어보세요</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="text-label-lg font-bold text-on-surface-variant hover:text-danger transition-colors px-3 py-1.5 neo-brutal-border rounded-xl"
            title="대화 초기화"
            @click="chat.clear()"
          >
            새 대화
          </button>
          <button
            class="w-10 h-10 flex items-center justify-center rounded-xl hover:bg-surface transition-colors"
            @click="chat.toggle()"
          >
            <span class="material-symbols-outlined text-on-surface-variant">close</span>
          </button>
        </div>
      </div>

      <!-- 메시지 목록 -->
      <div ref="messagesEl" class="flex-1 overflow-y-auto p-6 space-y-4">
        <div v-if="!chat.messages.length" class="text-center text-on-surface-variant py-12">
          <span class="material-symbols-outlined text-6xl block mb-4 opacity-20" style="font-variation-settings:'FILL' 1;">chat_bubble</span>
          <p class="text-body-md font-bold mb-1">"오늘 식단 어떻게 해야 할까요?"</p>
          <p class="text-body-md mb-3">"단백질 더 먹으려면 뭘 먹어야 해?"</p>
          <p class="text-label-lg opacity-50">오늘 식단 기록을 바탕으로 답해드려요</p>
        </div>

        <div
          v-for="(msg, i) in chat.messages"
          :key="i"
          class="flex"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <!-- AI 메시지: 마크다운 렌더링 -->
          <div
            v-if="msg.role === 'ai'"
            class="max-w-[88%] px-5 py-4 rounded-xl neo-brutal-border bg-white text-on-background prose-chat"
            v-html="renderMd(msg.content)"
          />
          <!-- 사용자 메시지 -->
          <div
            v-else
            class="max-w-[80%] px-5 py-4 rounded-xl neo-brutal-border bg-primary text-white text-body-md font-bold"
          >
            {{ msg.content }}
          </div>
        </div>

        <div v-if="chat.loading" class="flex justify-start">
          <div class="bg-white neo-brutal-border px-5 py-4 rounded-xl flex items-center gap-3">
            <span class="material-symbols-outlined animate-spin text-on-surface-variant">progress_activity</span>
            <span class="text-body-md text-on-surface-variant">생각 중...</span>
          </div>
        </div>
      </div>

      <!-- 입력창 -->
      <div class="p-5 border-t-[3px] border-on-background flex-shrink-0">
        <p v-if="chat.error" class="text-label-lg text-danger font-bold mb-3">{{ chat.error }}</p>
        <div class="flex gap-3">
          <input
            v-model="chat.input"
            type="text"
            placeholder="질문을 입력하세요..."
            class="flex-1 px-4 py-3.5 neo-brutal-border rounded-xl text-body-md focus:outline-none focus:ring-2 focus:ring-primary bg-white"
            :disabled="chat.loading"
            @keydown.enter.prevent="chat.send()"
          />
          <button
            class="w-14 h-14 bg-primary neo-brutal-border rounded-xl flex items-center justify-center flex-shrink-0 hover:-translate-y-0.5 transition-transform disabled:opacity-50 disabled:translate-y-0"
            :disabled="chat.loading || !chat.input.trim()"
            @click="chat.send()"
          >
            <span class="material-symbols-outlined text-white text-2xl" style="font-variation-settings:'FILL' 1;">send</span>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { marked } from 'marked'
import { useChatStore } from '@/stores/chat'

const chat = useChatStore()
const messagesEl = ref(null)

marked.setOptions({ breaks: true })

function renderMd(content) {
  return marked.parse(content)
}

watch(
  () => chat.messages.length,
  async () => {
    await nextTick()
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  }
)

watch(
  () => chat.isOpen,
  async (opened) => {
    if (opened) {
      await nextTick()
      if (messagesEl.value) {
        messagesEl.value.scrollTop = messagesEl.value.scrollHeight
      }
    }
  }
)
</script>

<style scoped>
/* 채팅 패널 열림/닫힘 애니메이션 */
.chat-panel-enter-active,
.chat-panel-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.chat-panel-enter-from,
.chat-panel-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
}

/* AI 응답 마크다운 스타일 */
:deep(.prose-chat) {
  font-size: 0.875rem;
  line-height: 1.6;
}
:deep(.prose-chat p) {
  margin: 0 0 0.5em;
}
:deep(.prose-chat p:last-child) {
  margin-bottom: 0;
}
:deep(.prose-chat strong) {
  font-weight: 800;
}
:deep(.prose-chat ul),
:deep(.prose-chat ol) {
  margin: 0.4em 0 0.4em 1.2em;
  padding: 0;
}
:deep(.prose-chat li) {
  margin-bottom: 0.2em;
}
:deep(.prose-chat h1),
:deep(.prose-chat h2),
:deep(.prose-chat h3) {
  font-weight: 800;
  margin: 0.6em 0 0.3em;
  line-height: 1.3;
}
:deep(.prose-chat h1) { font-size: 1.1em; }
:deep(.prose-chat h2) { font-size: 1.05em; }
:deep(.prose-chat h3) { font-size: 1em; }
:deep(.prose-chat code) {
  background: #f3f4f6;
  border-radius: 4px;
  padding: 0.1em 0.4em;
  font-size: 0.85em;
  font-family: monospace;
}
:deep(.prose-chat pre) {
  background: #f3f4f6;
  border-radius: 8px;
  padding: 0.75em 1em;
  overflow-x: auto;
  margin: 0.5em 0;
}
:deep(.prose-chat pre code) {
  background: none;
  padding: 0;
}
</style>
