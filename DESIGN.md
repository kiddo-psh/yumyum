---
name: 냠냠코치
description: 식단을 기록하며 냠냠이를 키우고, 코치에게 조언 받는 AI 건강 관리 웹 앱
colors:
  primary: "#FF8C42"
  on-primary: "#ffffff"
  background: "#FFFFFF"
  sub-background: "#FAFAFA"
  surface: "#F5F5F5"
  on-background: "#2D2D2D"
  on-surface-variant: "#8A8A8A"
  outline: "#E0E0E0"
  nyam-mint: "#A8E6CF"
  protein: "#6B9BD1"
  carbs: "#F4C95D"
  fat: "#E8806B"
  success: "#4CAF50"
  warning: "#FFA726"
  danger: "#EF5350"
typography:
  display:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "48px"
    fontWeight: 800
    lineHeight: 1.1
    letterSpacing: "-0.02em"
  display-md:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "36px"
    fontWeight: 800
    lineHeight: 1.2
  headline:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.3
  headline-md:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "22px"
    fontWeight: 700
    lineHeight: 1.4
  body:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: 1.6
  body-lg:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "18px"
    fontWeight: 500
    lineHeight: 1.6
  label:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "14px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.05em"
  numeral:
    fontFamily: "Plus Jakarta Sans, sans-serif"
    fontSize: "64px"
    fontWeight: 800
    lineHeight: 1
rounded:
  DEFAULT: "16px"
  lg: "32px"
  xl: "48px"
  full: "9999px"
spacing:
  base: "8px"
  gutter: "24px"
  margin: "40px"
  sidebar-width: "280px"
  max-width: "1440px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.DEFAULT}"
    padding: "12px 24px"
  button-primary-hover:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.DEFAULT}"
    padding: "8px 24px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.on-background}"
    rounded: "{rounded.DEFAULT}"
    padding: "12px 24px"
  nav-item-active:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.DEFAULT}"
    padding: "12px 16px"
  nav-item-default:
    backgroundColor: "transparent"
    textColor: "{colors.on-background}"
    rounded: "{rounded.DEFAULT}"
    padding: "12px 16px"
---

# Design System: 냠냠코치

## 1. Overview

**Creative North Star: "The Growth Journal"**

냠냠코치는 건강 기록을 일기처럼 쌓아가는 공간이다. 매 기록이 쌓여 뱃지가 붙고, 스트릭이 이어지고, 냠냠이가 자란다 — 성장 일지에 스티커가 채워지듯. 네오-브루탈 하드 엣지는 "진짜로 기록했다"는 물성 있는 신호이고, 순백 배경 위의 오렌지 코랄은 다음 기록을 향한 행동 에너지다.

이 시스템은 두 가지를 철저히 거부한다: 부드러운 뉴모피즘 건강 앱 미학(안심되는 파스텔과 둥근 그라데이션 카드)과 피트니스 앱의 고강도 에너지(강렬한 빨강, 어두운 배경, 마초 서체). 냠냠코치는 무겁지 않다 — 솔직하고 따뜻하고 리워딩하다.

**Key Characteristics:**
- 순백 캔버스 위의 잉크-블랙 3px 테두리 — 그래픽 직접성
- 활력 코랄이 화면의 10% 이하 — 희소성으로 주목도 유지
- 유쾌한 스피어민트는 냠냠이 전용 시그니처 컬러
- 6px 오프셋 섀도우는 클릭 가능한 요소에만 — 물성으로 상호작용 신호
- Plus Jakarta Sans 800 → 500 폭넓은 웨이트 범위로 위계 형성

## 2. Colors: 잉크 위의 코랄

순백 캔버스에 잉크-블랙 구조체가 얹히고, 활력 코랄이 행동을 표시하며, 유쾌한 스피어민트가 생명(냠냠이)을 표시하는 3층 팔레트.

### Primary
- **활력 코랄** (`#FF8C42`): CTA 버튼, 활성 네비게이션, 진행 표시기. 행동 에너지. 화면의 10% 이하로 유지한다.

### Secondary
- **유쾌한 스피어민트** (`#A8E6CF`): 냠냠이 마스코트 전용 배경색. AI 코치 기능·식단 분석 카드·일반 배경에는 사용하지 않는다. 두 캐릭터(냠냠이·냠냠코치)는 색으로 구분된다.

### Tertiary
- **영양소 트리오** — 차트·영양소 분석 화면에서만 사용. 세 색상이 동시에 보일 때만 의미 있다:
  - 단백질 (`#6B9BD1`): 차분한 스틸 블루
  - 탄수화물 (`#F4C95D`): 따뜻한 골든 옐로우
  - 지방 (`#E8806B`): 부드러운 살몬 코랄

### Neutral
- **잉크 블랙** (`#2D2D2D`): 텍스트, 네오-브루탈 테두리, 섀도우. 진짜 블랙 대신 따뜻한 다크그레이.
- **소프트 그레이** (`#8A8A8A`): 보조 텍스트, 비활성 레이블. 흰 배경 대비 4.5:1 확보.
- **순백 캔버스** (`#FFFFFF`): 메인 배경. 크림·샌드·아이보리 금지.
- **오프-화이트** (`#FAFAFA`): 사이드바, 서브 패널.
- **서피스** (`#F5F5F5`): 호버 상태, 비활성 컨테이너.
- **아웃라인** (`#E0E0E0`): 구분선, 비강조 테두리.
- **성공** (`#4CAF50`) / **경고** (`#FFA726`) / **위험** (`#EF5350`): 상태 메시지 전용. 데코레이션 금지.

### Named Rules

**The Coral Scarcity Rule.** 활력 코랄(`#FF8C42`)은 화면당 1-2개 요소에만. CTA 버튼과 활성 네비 항목 이외에 코랄을 추가하면 즉시 재검토한다. 희소성이 에너지다.

**The Mint Ownership Rule.** 스피어민트(`#A8E6CF`)는 냠냠이 캐릭터 전용이다. AI 코치 기능, 식단 분석, 일반 UI에 사용하면 두 캐릭터의 정체성이 섞인다.

**The Dead Token Warning.** `frontend/src/styles/tokens.css`의 초록 팔레트(`#58cc02`)는 임포트되지 않으며 현재 시스템의 일부가 아니다. 그 파일의 값을 사용하지 않는다.

## 3. Typography: 단일 패밀리, 다중 웨이트

**Display/Body Font:** Plus Jakarta Sans (sans-serif)

단일 패밀리 전략 — 800 Extra Bold에서 500 Medium까지 4단계 웨이트로 전체 위계를 형성한다. 기하학적 구조가 네오-브루탈 테두리와 같은 문법을 공유한다. 세리프나 모노스페이스 혼합 없음.

**Character:** 수치는 존재감 있게, 레이블은 정밀하게, 본문은 읽기 편하게. 게임의 에너지와 기록 앱의 목적성을 같은 폰트 패밀리 안에서 조율한다.

### Hierarchy
- **Display Large** (800, 48px, lh 1.1, ls -0.02em): 온보딩 히어로, 주요 섹션 제목. 화면당 1개.
- **Display Medium** (800, 36px, lh 1.2): 홈 섹션 주요 헤딩.
- **Headline** (700, 28px, lh 1.3): 카드 제목, 섹션 헤딩.
- **Headline Medium** (700, 22px, lh 1.4): 서브섹션, 대화형 패널 제목.
- **Body Large** (500, 18px, lh 1.6): 주요 본문, 설명 텍스트.
- **Body** (500, 16px, lh 1.6): 일반 본문. 최대 65ch.
- **Label** (700, 14px, lh 1.2, ls 0.05em): 버튼 텍스트, 네비 레이블, 칩, 배지.
- **Numeral** (800, 64px, lh 1): 칼로리 숫자, 스트릭 카운터. 수치 전용 스케일.

### Named Rules

**The Label-Not-Caps Rule.** 레이블 텍스트는 0.05em 자간을 사용하되 `text-transform: uppercase` 금지. 영문 레이블에 대문자 변환은 경직된 인상을 주고, 한글 레이블에는 아무 효과도 없다.

**The Numeral Reservation Rule.** 64px 800 Numeral 스케일은 칼로리·스트릭·운동 수치 등 "게임에서 이긴 숫자"에만 사용한다. 일반 헤딩에 적용하면 위계가 무너진다.

## 4. Elevation: Structural Lift

이 시스템은 **Structural Lift** 전략을 사용한다: 그림자는 평면 기본 상태에 없고, 클릭·탭 가능한 상호작용 요소에만 나타난다. 그림자가 보인다 = 누를 수 있다.

앰비언트 디퓨즈 섀도우(blur 있는 부드러운 그림자)는 사용하지 않는다. 오직 하드 오프셋 섀도우만 — 이것이 네오-브루탈의 물성 신호다.

### Shadow Vocabulary
- **Neo-Brutal Rest** (`box-shadow: 6px 6px 0px 0px #2D2D2D`): 버튼, CTA 카드, FAB의 기본 상태.
- **Neo-Brutal Hover** (`box-shadow: 10px 10px 0px 0px #2D2D2D` + `transform: translate(-4px, -4px)`): hover 시 섀도우 확장 + 카드 부양. 300ms ease-out-quart.
- **Neo-Brutal Press** (`box-shadow: none` + `transform: translate(6px, 6px)`): active 시 눌린 물성.

### Named Rules

**The Flat-By-Default Rule.** 배경, 텍스트 영역, 비상호작용 컨테이너에는 섀도우 없음. 정보 표시 영역이 뜨면 계층 구조가 무너진다.

**The No-Blur Rule.** `box-shadow` blur radius 금지. `6px 6px 0px 0px` — 마지막 두 값은 항상 0. 블러는 네오-브루탈이 아니다.

## 5. Components

### Buttons

**냠냠코치 버튼은 물리적으로 돌출되어 있다.** 누를 수 있음이 보인다.

- **Shape:** 16px radius (rounded default). 날카로운 모서리 금지.
- **Primary:** 활력 코랄 배경 + 흰 텍스트 + 3px 잉크-블랙 테두리 + 6px 오프셋 섀도우. Padding 12px 24px.
- **Hover:** translate(-4px, -4px) + 10px 섀도우 확장 (300ms ease-out-quart).
- **Press/Active:** translate(6px, 6px) + 섀도우 제거.
- **Ghost:** 투명 배경 + 잉크-블랙 테두리 + 잉크-블랙 텍스트. 동일 hover/press 처리.
- **FAB (Floating Action Button):** 80px circle, primary 배경, neo-brutal-shadow. 화면 우하단 고정. z-50.

### Cards / Containers

- **Corner Style:** 16px radius (rounded default). 중첩 카드 내부는 8-12px.
- **Background:** 흰 배경 기본. 서브패널은 `#FAFAFA`.
- **Shadow Strategy:** 상호작용 카드만 neo-brutal-shadow. 정보 표시 카드는 3px 테두리만, 섀도우 없음.
- **Border:** `3px solid #2D2D2D` (neo-brutal-border) — 모든 상호작용 카드 필수.
- **Hover:** neo-brutal-card-hover 클래스 — translate(-4px, -4px) + 섀도우 10px 확장.
- **Nested Cards:** 절대 금지. 카드 안에 카드 구조 대신 padding + divider + 배경색 차분.

### Inputs / Fields

- **Style:** `2px solid #E0E0E0` 아웃라인, 10px radius, 흰 배경.
- **Focus:** `border-color: #2D2D2D` — 잉크-블랙으로 강화. 글로우·색상 변화 없음.
- **Label:** 14px 700, 필드 상단 위치.
- **Error:** `text-danger` (`#EF5350`) 인라인 메시지. 필드 하단.
- **Disabled:** `#8A8A8A` 텍스트 + `#F5F5F5` 배경.

### Navigation (Sidebar)

- **Default item:** 투명 배경, 잉크-블랙 텍스트, 아이콘(Material Symbols) + 레이블 병기. 아이콘 단독 금지.
- **Active item:** 활력 코랄 배경 + 흰 텍스트 + neo-brutal-border + `-translate-y-1` 부양.
- **Hover:** `#F5F5F5` 서피스 배경 + `-translate-y-0.5` 미세 부양.
- **Mobile treatment:** transform translateX 슬라이드 드로어 (300ms ease-out-quart). 1024px 미만 자동 닫힘.

### Badge Strip (Signature Component)

뱃지 도감 프리뷰 — 36px 이미지 그리드로 사이드바 하단에 고정. 최대 5개 표시, 초과 시 `+N` 카운터. hover 시 `-translate-y-0.5` 부양. 획득 시 BadgeCelebrationOverlay 연출 후 컬렉션 즉시 갱신.

### Progress Circle (Signature Component)

칼로리 달성률 SVG 링 — `role="progressbar"`, `stroke-dashoffset` 1.5s ease-out-quart 전환. rotate(-90deg)으로 12시 방향 시작. 링 센터에 64px 800 Numeral로 수치 표시.

## 6. Do's and Don'ts

### Do:
- **Do** `neo-brutal-border` (`3px solid #2D2D2D`)를 모든 상호작용 요소(버튼, 활성 네비 항목, CTA 카드)에 적용한다.
- **Do** 섀도우는 클릭 가능한 요소에만. blur radius는 항상 0.
- **Do** 숫자(칼로리, 스트릭, 달성 수치)는 64px 800 Numeral 스케일을 사용한다.
- **Do** 스피어민트(`#A8E6CF`)는 냠냠이 마스코트 전용으로 보호한다.
- **Do** 활성 네비 항목의 `-translate-y-1` 부양을 유지한다 — 현재 위치의 물성 신호.
- **Do** 모든 트랜지션에 `prefers-reduced-motion: reduce` 대안을 제공한다.
- **Do** 중첩 라우트 활성 감지는 `startsWith` 기반으로 처리한다 (`/routine/detail`에서 `/routine` 항목이 활성화되어야 함).

### Don't:
- **Don't** 파스텔 그라데이션, 뉴모피즘 소프트 섀도우, 글래스모피즘 카드를 사용한다 — 이 시스템의 정반대다.
- **Don't** 어두운 배경 + neon 액센트를 사용한다 — 피트니스 앱 마초 에너지는 냠냠코치가 아니다.
- **Don't** `box-shadow`에 blur를 넣는다 (`6px 6px 0px 0px` — 마지막 두 값은 항상 0).
- **Don't** 활력 코랄(`#FF8C42`)을 배경색, 대형 섹션 채움, 또는 텍스트 색으로 사용한다.
- **Don't** 레이블에 `text-transform: uppercase`를 적용한다 — `letterSpacing: 0.05em`으로 충분하다.
- **Don't** 중첩 카드(카드 안의 카드)를 만든다. padding과 divider로 분리한다.
- **Don't** `background-clip: text` + gradient로 그라데이션 텍스트를 만든다.
- **Don't** 크림·샌드·아이보리 배경색을 사용한다. 배경은 `#FFFFFF`다.
- **Don't** 섹션마다 번호 마커(01/02/03)나 소형 대문자 아이브로우(`ABOUT`, `PROCESS`)를 붙인다.
- **Don't** `frontend/src/styles/tokens.css`의 초록 팔레트(`#58cc02`)를 사용한다 — 미임포트, 미사용 파일이다.
