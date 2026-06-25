<template>
  <main class="login-bg">

    <!-- 브랜드 워드마크 -->
    <div class="login-inner">
      <div class="login-brand" aria-label="냠냠코치">
        <span class="login-brand-ko">냠냠코치</span>
        <span class="login-brand-en">NYAMNYAM COACH</span>
      </div>

      <!-- 투톤 카드 (가로 분할) -->
      <div class="login-card neo-brutal-border rounded-xl overflow-hidden">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-px bg-on-background">

          <!-- 왼쪽: 민트 존 -->
          <div class="bg-nyam-mint px-14 py-14 flex flex-col items-center justify-center gap-8">
            <div class="speech-bubble">
              <span class="text-body-md font-bold text-on-background">오늘도 냠냠하러 오셨군요!</span>
            </div>
            <div class="nyam-bob" aria-hidden="true">
              <img src="/nyam/nyam_logo.png" alt="" width="200" height="200" class="object-contain" />
            </div>
          </div>

          <!-- 오른쪽: 흰 존 -->
          <div class="bg-white px-12 py-14 flex flex-col justify-center gap-10">
            <div>
              <h1 class="text-display-md font-bold text-on-background mb-4" style="line-height: 1.1;">
                기록하면<br>냠이가 자랍니다
              </h1>
              <p class="text-body-lg text-on-surface-variant">
                식단·운동·스트릭을 한 곳에서
              </p>
            </div>

            <div>
              <button
                type="button"
                class="kakao-btn rounded-xl w-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
                @click="onKakaoLogin"
              >
                <svg class="kakao-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 3C6.99 3 3 6.2 3 10.13c0 2.5 1.66 4.7 4.16 5.96-.18.62-.66 2.27-.76 2.62-.12.43.16.43.34.31.14-.09 2.2-1.49 3.1-2.1.7.1 1.42.16 2.16.16 5.01 0 9-3.2 9-7.95C21 6.2 17.01 3 12 3Z" fill="#191600"/>
                </svg>
                <span>카카오로 시작하기</span>
              </button>
              <p class="text-label-lg text-on-surface-variant text-center mt-5">
                30초면 충분해요 · 무료로 시작하세요
              </p>
            </div>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { startKakaoLogin } from '@/services/auth';

function onKakaoLogin() {
  startKakaoLogin();
}
</script>

<style scoped>
/* ── 페이지 배경 ── */
.login-bg {
  min-height: 100vh;
  background-color: #2D2D2D;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.login-inner {
  width: 100%;
  max-width: 760px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

/* ── 브랜드 워드마크 ── */
.login-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.login-brand-ko {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.02em;
}

.login-brand-en {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.4);
}

/* ── 카드 ── */
.login-card {
  width: 100%;
}

/* ── 말풍선 ── */
.speech-bubble {
  position: relative;
  background: #fff;
  border: 3px solid #2D2D2D;
  border-radius: 9999px;
  padding: 12px 24px;
  box-shadow: 4px 4px 0 0 #2D2D2D;
}

.speech-bubble::after {
  content: '';
  position: absolute;
  bottom: -14px;
  left: 50%;
  width: 14px;
  height: 14px;
  background: #fff;
  border-right: 3px solid #2D2D2D;
  border-bottom: 3px solid #2D2D2D;
  transform: translateX(-50%) rotate(45deg);
}

/* ── 냠냠이 바운스 ── */
.nyam-bob {
  animation: nyam-bob 3.2s ease-in-out infinite;
}

@keyframes nyam-bob {
  0%, 100% { transform: translateY(0); }
  50%       { transform: translateY(-12px); }
}

/* ── 카카오 버튼 ── */
.kakao-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 24px;
  background: #fee500;
  border: 3px solid #2D2D2D;
  box-shadow: 6px 6px 0 0 #2D2D2D;
  font-size: 18px;
  font-weight: 800;
  color: #2D2D2D;
  transition: transform 120ms ease, box-shadow 120ms ease;
}

.kakao-btn:hover {
  transform: translate(-2px, -2px);
  box-shadow: 8px 8px 0 0 #2D2D2D;
}

.kakao-btn:active {
  transform: translate(4px, 4px);
  box-shadow: 2px 2px 0 0 #2D2D2D;
}

.kakao-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

/* ── 모바일 ── */
@media (max-width: 640px) {
  .login-bg { padding: 32px 16px; }
  .login-brand-ko { font-size: 22px; }
}

/* ── 모션 감소 ── */
@media (prefers-reduced-motion: reduce) {
  .nyam-bob { animation: none; }
  .kakao-btn { transition: none; }
}
</style>
