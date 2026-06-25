---
target: frontend/src/layouts/MainLayout.vue
total_score: 20
p0_count: 0
p1_count: 3
timestamp: 2026-06-25T02-21-39Z
slug: frontend-src-layouts-mainlayout-vue
---
## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Active nav 명확, 사이드바 상태 시각적 표현 있음. 그러나 `aria-expanded` 없음, 로그아웃 서버 오류 시 아무 피드백 없음 |
| 2 | Match System / Real World | 2 | `냠냠마스터 / Lv.12 훈련사` 하드코딩 — 실제 사용자 데이터 표시 안 됨. 영문 `Coaching Buddy` 혼재 |
| 3 | User Control and Freedom | 2 | 모바일 오버레이 클릭 닫기·내부 닫기 버튼 있음. Escape 키 없음, 키보드 단축키 없음, 데스크탑 리사이즈 시 사용자 선호 무시 |
| 4 | Consistency and Standards | 3 | 네비 아이템 스타일 일관. 외부 햄버거-내부 셰브론 비대칭은 드로어 표준 패턴. 뱃지 링크 `title` vs `aria-label` 불일치 |
| 5 | Error Prevention | 2 | 로그아웃 확인 없음, 뱃지 로드 실패 시 UI 피드백 없음 |
| 6 | Recognition Rather Than Recall | 3 | 모든 네비 아이템에 아이콘+텍스트, 명확한 활성 상태. 햄버거 버튼 항상 예상 위치 |
| 7 | Flexibility and Efficiency | 1 | 사이드바 토글 키보드 단축키 없음. Escape 키 미지원. 중첩 라우트(/routine/123)에서 네비 활성화 안 됨 |
| 8 | Aesthetic and Minimalist Design | 3 | 사이드바 레이아웃 깔끔. 섹션 분리 명확. 하드코딩된 프로필 데이터가 유일한 콘텐츠 이상 |
| 9 | Error Recovery | 1 | 로그아웃 서버 오류 조용히 삼킴(의도적이나 사용자 알림 없음). 뱃지 컬렉션 로드 실패 시 빈 화면 |
| 10 | Help and Documentation | 1 | 툴팁·온보딩·도움말 없음. 사이드바 접기/펼치기 기능 발견 불가 |
| **Total** | | **20/40** | **Acceptable (낮은 경계선)** |

---

## Anti-Patterns Verdict

**LLM assessment**: 사이드바 레이아웃 자체는 AI 슬롭 징후 없음. 아이콘+텍스트 네비, 브랜드 헤더, 사용자 프로필 카드 — 의미 있는 구성이며 앱 고유 요소(뱃지 프리뷰 스트립, neo-brutal-border 스타일)가 개성을 만들고 있음. `Coaching Buddy` 영문 부제가 약간 제너릭하게 읽히나 브랜드 선택으로 해석 가능. 전반적으로 AI가 만든 느낌은 없음.

**Deterministic scan**: `detect.mjs` 0건 — 금지된 패턴(그라데이션 텍스트, 글래스모피즘, 아이콘 카드 그리드 등) 없음. 전 파일 클린.

**Visual overlays**: 브라우저 자동화 미사용 환경이므로 인젝션 미수행.

---

## Overall Impression

사이드바 트랜지션은 300ms ease-out-quart로 쾌적하고, 모바일 라우트 변경 시 자동 닫힘 패턴도 세심하다. 그러나 **프로필 섹션이 완전히 허구 데이터**("냠냠마스터 / Lv.12 훈련사")를 표시하고 있어 앱의 개인화 약속을 즉각 깨뜨린다. ARIA 지원도 절반만 구현된 상태 — 시각적 상태 전환은 있지만 보조 기술에는 전달되지 않음.

---

## What's Working

1. **사이드바 애니메이션 타이밍**: 300ms `cubic-bezier(0.22, 1, 0.36, 1)` + 메인 콘텐츠 margin-left 동기 전환. 레이아웃 리플로우 없이 transform만 사용해 성능 안전.
2. **네비 텍스트+아이콘 병기**: 아이콘 단독이 아닌 레이블 표시로 인지 부하 최소화. 활성 상태의 `-translate-y-1 + bg-primary` 조합이 명확한 위치 신호를 줌.
3. **모바일 라우트 변경 시 자동 닫힘**: `watch(() => route.path)` 패턴이 모바일 UX의 흔한 통증 지점을 선제적으로 해결.

---

## Priority Issues

### [P1] 하드코딩된 사용자 프로필 데이터

**What**: 89번 줄 `냠냠마스터`, 91번 줄 `Lv.12 훈련사`가 리터럴 문자열.

**Why it matters**: 모든 사용자가 동일한 이름과 레벨을 보게 됨. 개인화된 건강 관리 앱에서 신뢰를 즉각 깨뜨리고, 실제 데이터 연동 여부를 알 수 없게 만듦.

**Fix**: `useMemberStore()` 또는 `useAuthStore()`에서 실제 닉네임과 레벨을 바인딩. 로딩 상태와 빈 값 폴백 처리 필요.

**Suggested command**: `/impeccable harden`

---

### [P1] ARIA: 사이드바 접근성 3종 누락

**What**: (a) `<aside>` 에 `aria-label` 없음 — 스크린 리더가 "탐색 영역" 구별 불가. (b) 햄버거 버튼에 `aria-expanded` 없음 — 열림/닫힘 상태 전달 안 됨. (c) 사이드바 닫힐 때 포커스가 햄버거 버튼으로 돌아오지 않음 — 키보드 사용자가 빈 공간에 떨어짐.

**Why it matters**: 시각적으로 명확한 상태 전환이 보조 기술에는 전혀 전달되지 않음. Sam(접근성 의존 사용자)은 사이드바가 열렸는지조차 알 수 없음.

**Fix**:
```html
<aside aria-label="주 내비게이션" ...>
```
```html
<button :aria-expanded="sidebarOpen" aria-controls="main-sidebar" aria-label="메뉴 열기" ...>
```
```js
// closeSidebar 후 포커스 반환
function closeSidebar() {
  sidebarOpen.value = false
  nextTick(() => hamburgerRef.value?.focus())
}
```

**Suggested command**: `/impeccable audit`

---

### [P1] 중첩 라우트에서 네비 활성 상태 깨짐

**What**: 49번 줄 `$route.path === item.to` — 정확한 문자열 비교. `/routine/detail/123` 방문 시 "운동 루틴" 항목이 활성화되지 않음.

**Why it matters**: 사용자가 어느 섹션에 있는지 알 수 없음. 현재 위치 신호의 핵심 역할이 무너짐.

**Fix**: `startsWith` 또는 Vue Router의 `useLink` 활용:
```js
const isActive = (to) => to === '/' ? route.path === '/' : route.path.startsWith(to)
```

**Suggested command**: `/impeccable harden`

---

### [P2] 데스크탑 리사이즈 시 사이드바 사용자 선호 무시

**What**: 173–178번 줄 `handleResize()`가 `>= LG`이면 항상 `sidebarOpen = true`. 사용자가 의도적으로 닫아도 창 너비 조정 시 강제 재오픈.

**Why it matters**: 넓은 화면에서 콘텐츠 집중을 위해 사이드바를 닫은 파워 유저에게 반복적 마찰 발생.

**Fix**: 리사이즈 시 `< LG` 케이스에서만 닫기, `>= LG`에서는 현재 상태 유지:
```js
function handleResize() {
  const lg = window.innerWidth >= LG
  isLg.value = lg
  if (!lg) sidebarOpen.value = false
  // lg일 때 sidebarOpen 건드리지 않음
}
```

**Suggested command**: `/impeccable harden`

---

### [P2] `prefers-reduced-motion` 미지원

**What**: 235–253번 줄 사이드바 슬라이드 트랜지션(300ms)과 오버레이 페이드(200ms)에 `@media (prefers-reduced-motion: reduce)` 블록 없음. 상태는 `asideStyle` 인라인 스타일로 적용되어 CSS 미디어 쿼리만으로는 무력화 불가.

**Why it matters**: 전정 장애나 광과민성 사용자에게 빠른 화면 슬라이드 애니메이션이 불쾌감 유발. WCAG 2.1 Success Criterion 2.3.3 (AAA).

**Fix**: `TRANSITION` 상수를 조건부로:
```js
const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const TRANSITION = prefersReduced ? 'none' : 'transform 300ms cubic-bezier(0.22, 1, 0.36, 1)'
```
CSS 전환에도 동일한 미디어 쿼리 블록 추가.

**Suggested command**: `/impeccable animate`

---

## Persona Red Flags

### Alex (Power User)

Alex는 키보드로 사이드바를 토글하려 함 → 단축키 없음, Tab 순환으로는 접근 불가. Escape 키로 사이드바 닫기 시도 → 동작 없음. `/routine/sessions` 페이지에서 작업 중 네비의 "운동 루틴"이 비활성화된 것을 보고 현재 위치를 잃음. 데스크탑에서 집중을 위해 사이드바 닫기 → 윈도우 스냅 조정 시 재오픈.

### Sam (Accessibility-Dependent User)

사이드바 열릴 때 스크린 리더가 상태 변화 안내 없음 (`aria-expanded` 없음). `<aside>` 역할 미표시로 "내비게이션 랜드마크"로 인식 안 됨. 모바일에서 사이드바 닫기 버튼 활성화 후 포커스가 페이지 어딘가로 날아감 — 다음 탭 대상 예측 불가. 뱃지 링크(`<RouterLink title="뱃지 도감 보기">`)의 `title`은 스크린 리더에서 불안정하게 읽힘.

### Casey (Distracted Mobile User)

앱으로 돌아왔을 때 사이드바가 닫혀 있고 오른쪽 하단 AI FAB만 보임. 왼쪽 상단 고정 햄버거 버튼이 페이지 콘텐츠 위에 겹쳐 있어 처음에는 페이지 콘텐츠인지 네비 버튼인지 구분 어려움. 터치 타겟은 44×44px (`w-11 h-11 = 44px`) — 기준에 부합.

---

## Minor Observations

- **`gap-base` (14번 줄)**: `base`는 Tailwind 기본 스케일에 없는 토큰. 커스텀 토큰이 있다면 무관하나, 없다면 `gap-0`으로 해석됨. `padding: 24px`가 실제 내부 간격을 담당하고 있어 시각적 영향은 없지만 명확히 해야 함.
- **`text-[10px]` (91번 줄)**: "Lv.12 훈련사" 텍스트가 10px. WCAG 정상 텍스트 최소 권장 크기(14px)에 한참 못 미침. 12px로도 부족; `text-xs`(12px) 이상 권장.
- **`handleResize` 디바운스 없음 (173번 줄)**: 빠른 리사이즈 시 매 프레임마다 상태 업데이트. 성능 문제는 미미하지만 `debounce(handleResize, 100)`으로 안전하게 처리 가능.
- **로그아웃 확인 다이얼로그 없음**: 앱 특성상(건강 기록 앱) 의도치 않은 로그아웃 시 재로그인 마찰이 있음. 최소한 `aria-describedby`로 부작용 안내 고려.
- **z-index 시멘틱 스케일**: overlay(z-30) → sidebar(z-40) → hamburger+FAB(z-50). 드롭다운이나 모달이 추가될 경우 z-50 이상 레이어 없음. 지금은 무해하나 성장 시 충돌 위험.

---

## Questions to Consider

- 하드코딩 프로필 데이터("냠냠마스터")는 개발 편의를 위한 플레이스홀더인가, 아니면 실제 사용자 스토어 연동이 아직 미구현인가? 현재 `useAuthStore`나 `useMemberStore`에 닉네임 필드가 있는가?
- 사이드바가 닫힌 상태에서 페이지 콘텐츠가 왼쪽 상단 44px 영역을 비워야 하는가, 아니면 햄버거 버튼이 콘텐츠 위에 부유하는 것이 의도된 디자인인가?
- `Coaching Buddy` 부제는 냠냠코치 캐릭터 확정 전 임시 텍스트인가, 브랜드 결정인가? 캐릭터 분리 후 이 공간이 어떻게 활용될지 미리 계획되어 있는가?
