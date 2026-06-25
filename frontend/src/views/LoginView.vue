<template>
  <main class="login">
    <div class="login__stage">
      <!-- 코치 인사: 캐릭터가 말을 거는 순간 -->
      <div class="login__greeter">
        <p class="login__bubble">
          오늘도 냠냠하러 오셨군요!
        </p>
        <div
          class="login__nyam"
          aria-hidden="true"
        >
          <svg
            viewBox="0 0 120 120"
            class="login__nyam-art"
            role="presentation"
          >
            <!-- 새싹 -->
            <path
              d="M60 20 C60 8 70 4 76 8 C78 16 70 22 60 22 Z"
              fill="#FF8C42"
              stroke="#2D2D2D"
              stroke-width="3"
              stroke-linejoin="round"
            />
            <!-- 몸통 -->
            <path
              d="M28 64 C28 40 48 28 60 28 C72 28 92 40 92 64 C92 90 78 104 60 104 C42 104 28 90 28 64 Z"
              fill="#A8E6CF"
              stroke="#2D2D2D"
              stroke-width="3"
              stroke-linejoin="round"
            />
            <!-- 볼 -->
            <circle
              cx="42"
              cy="74"
              r="6"
              fill="#FF8C42"
              opacity="0.55"
            />
            <circle
              cx="78"
              cy="74"
              r="6"
              fill="#FF8C42"
              opacity="0.55"
            />
            <!-- 눈 -->
            <circle
              cx="49"
              cy="62"
              r="5"
              fill="#2D2D2D"
            />
            <circle
              cx="71"
              cy="62"
              r="5"
              fill="#2D2D2D"
            />
            <circle
              cx="50.6"
              cy="60.4"
              r="1.6"
              fill="#ffffff"
            />
            <circle
              cx="72.6"
              cy="60.4"
              r="1.6"
              fill="#ffffff"
            />
            <!-- 입 -->
            <path
              d="M52 76 Q60 84 68 76"
              fill="none"
              stroke="#2D2D2D"
              stroke-width="3"
              stroke-linecap="round"
            />
          </svg>
        </div>
      </div>

      <!-- 가치 제안 -->
      <div class="login__intro">
        <h1 class="login__title">
          기록하면<br>냠이가 자랍니다
        </h1>
        <p class="login__subtitle">
          식단·운동·스트릭까지, 냠냠코치의 모든 기능은<br>로그인 후 바로 이어집니다.
        </p>
      </div>

      <!-- 단 하나의 행동 -->
      <div class="login__action">
        <button
          type="button"
          class="kakao-btn"
          @click="onKakaoLogin"
        >
          <svg
            class="kakao-btn__icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              d="M12 3C6.99 3 3 6.2 3 10.13c0 2.5 1.66 4.7 4.16 5.96-.18.62-.66 2.27-.76 2.62-.12.43.16.43.34.31.14-.09 2.2-1.49 3.1-2.1.7.1 1.42.16 2.16.16 5.01 0 9-3.2 9-7.95C21 6.2 17.01 3 12 3Z"
              fill="#191600"
            />
          </svg>
          <span>카카오로 시작하기</span>
        </button>
        <p class="login__reassure">
          30초면 충분해요 · 카카오 계정으로 바로 시작
        </p>
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
.login {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 32px 20px;
  background: var(--color-nyam-mint, #a8e6cf);
  background:
    radial-gradient(circle at 50% 120%, #ffffff 0%, transparent 55%),
    var(--color-nyam-mint, #a8e6cf);
}

.login__stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 420px;
  text-align: center;
}

/* 캐릭터 인사 */
.login__greeter {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 28px;
}

.login__bubble {
  position: relative;
  margin: 0 0 18px;
  padding: 10px 18px;
  border: 3px solid #2d2d2d;
  border-radius: 9999px;
  background: #ffffff;
  box-shadow: 4px 4px 0 0 #2d2d2d;
  font-size: 15px;
  font-weight: 700;
  color: #2d2d2d;
}

.login__bubble::after {
  content: '';
  position: absolute;
  bottom: -11px;
  left: 50%;
  width: 14px;
  height: 14px;
  background: #ffffff;
  border-right: 3px solid #2d2d2d;
  border-bottom: 3px solid #2d2d2d;
  transform: translateX(-50%) rotate(45deg);
}

.login__nyam {
  display: grid;
  place-items: center;
  width: 168px;
  height: 168px;
  border-radius: 9999px;
  background: #ffffff;
  border: 3px solid #2d2d2d;
  box-shadow: 6px 6px 0 0 #2d2d2d;
  animation: nyam-bob 3.2s ease-in-out infinite;
}

.login__nyam-art {
  width: 116px;
  height: 116px;
}

/* 가치 제안 */
.login__intro {
  margin-bottom: 36px;
}

.login__title {
  margin: 0 0 14px;
  font-size: 38px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: #2d2d2d;
}

.login__subtitle {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.6;
  color: #2d2d2d;
}

/* 단일 행동 */
.login__action {
  width: 100%;
}

.kakao-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 16px 20px;
  border: 3px solid #2d2d2d;
  border-radius: 16px;
  background: #fee500;
  box-shadow: 6px 6px 0 0 #2d2d2d;
  font-size: 17px;
  font-weight: 800;
  color: rgba(0, 0, 0, 0.85);
  transition:
    transform 120ms ease,
    box-shadow 120ms ease;
}

.kakao-btn:hover {
  transform: translate(-2px, -2px);
  box-shadow: 8px 8px 0 0 #2d2d2d;
}

.kakao-btn:active {
  transform: translate(4px, 4px);
  box-shadow: 2px 2px 0 0 #2d2d2d;
}

.kakao-btn:focus-visible {
  outline: 3px solid #ff8c42;
  outline-offset: 3px;
}

.kakao-btn__icon {
  width: 22px;
  height: 22px;
}

.login__reassure {
  margin: 14px 0 0;
  font-size: 13px;
  font-weight: 600;
  color: #2d2d2d;
}

@keyframes nyam-bob {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login__nyam {
    animation: none;
  }
  .kakao-btn {
    transition: none;
  }
}

@media (max-width: 480px) {
  .login__title {
    font-size: 32px;
  }
  .login__nyam {
    width: 144px;
    height: 144px;
  }
}
</style>
