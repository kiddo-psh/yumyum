# 뱃지 프론트엔드 — 작업 계획 & 진행 기록

> API 계약·UX 흐름은 `BADGE_FRONTEND_HANDOFF.md` 참고. 이 문서는 **프론트 구현 계획과 남은 작업 추적**용.
> 결정: 전역 상태는 **Pinia 스토어**로 관리 (CLAUDE.md 컨벤션). `src/stores`의 첫 스토어가 됨.

## 사전 확인된 사실
- 컬렉션: `GET /badges` (`api/badge.js` 신규 필요).
- 획득 순간: 식사/운동 기록 응답에 piggyback (`newlyEarnedBadges`, `streak`). 별도 통신 없음.
- ⚠️ **운동 세션 기록 UI 부재**: `POST /routines/sessions` 호출부가 프론트에 없음. `RoutineView`는 온보딩/AI 루틴 생성만 함. → 운동 쪽 뱃지 연출은 세션 기록 화면이 선행돼야 함.
- 식단 기록 호출부는 존재: `recordMeal`(MealActionView), `recordPhotoMeal`(MealPhotoView).
- App.vue는 `<RouterView/>`만 → 전역 오버레이는 `MainLayout.vue`에 마운트.
- `POST /meals/{id}/items`(addMealItem), `POST /meals/photo/analyze`는 뱃지/스트릭 안 줌 → 연출 연결 제외.

## 작업 항목 (요청 3종)

### 0. 공통 기반
- [x] `src/api/badge.js` — `getBadgeCollection()` → `apiClient.get('/badges')`
- [x] `src/stores/badge.js` (Pinia) — 획득 연출 큐(`celebrate(response)`로 기록 응답 투입)·순차 표시 상태, 컬렉션 캐시(`loadCollection`)
- [x] `src/components/badge/BadgeCelebrationOverlay.vue` — 스토어 구독, `MainLayout.vue`에 1회 마운트

### 1. 마이페이지 뱃지 도감 (①)
- [x] `src/components/my/BadgeCollectionSection.vue` — `getBadgeCollection()` 렌더, `category`(DIET/WORKOUT/STREAK) 그룹핑, `earned`로 획득/잠김(회색) 구분. 잠긴 것도 name·icon·description 노출. 하드코딩 금지.
- [x] `MyView.vue`에 섹션 추가(ProgramSection 아래)

### 2. 사이드바 획득 뱃지 표기 (②)
- [x] `MainLayout.vue` User Profile 카드 근처에 획득 뱃지 미니 스트립(earned:true 필터, 아이콘 N개 + 더보기 → `/my`)

### 3. 기록 시 뱃지 획득 화면 (③)
- [x] BadgeCelebrationOverlay UX: `streak.increased` → 스트릭 갱신 화면 → `newlyEarnedBadges` 순차 모달 (HANDOFF §3)
- [x] 식단 연결: `MealActionView`(recordMeal)·`MealPhotoView`(recordPhotoMeal)·`LogView`(recordMeal) 응답 → `badgeStore.celebrate(...)`
- [ ] 운동 연결: **세션 기록 화면 구현 후** 동일 연결 (아래 TODO 의존)

## 남은/미구현 뱃지 화면 (구현 필요)
- [ ] **운동 세션 기록 화면 (`POST /routines/sessions`)** — 현재 프론트에 없음. 구현 후 ③ 운동 piggyback 연결.
- [ ] **스트릭 갱신 전용 연출** — 마일스톤(3·7·30·100일) 강조/애니메이션 디테일.

## 보류 (나중으로 미룸 — 알림·커뮤니티)
- [ ] **G603 뱃지 알림** 연동 — 별도 알림 도메인. (알림 기능 보류)
- [ ] **뱃지 상세/공유 화면** — 커뮤니티 연계. (커뮤니티 기능 보류)
