# Home Header + 마지막 끼니 추천 통합 설계

**날짜:** 2026-06-25
**파일:** `frontend/src/views/HomeView.vue`

## 배경

홈 화면 Row 2의 "마지막 끼니 추천" 카드는 이미지 없이 아이콘만 표시되어 공간 낭비가 있었다.
헤더의 `coachMessage`(AI 응원 멘트)와 통합하면 냠냠코치가 응원도 하고 끼니 추천도 자연스럽게 건네는 흐름이 만들어진다.

## 변경 범위

`frontend/src/views/HomeView.vue` 단일 파일 수정.

## 헤더 상태 전환

`coachMessage` `<p>` 자리를 3가지 상태로 분기:

| 상태 | 조건 | 표시 |
|---|---|---|
| 기본 | 기본 | AI 응원 멘트 텍스트 |
| 로딩 | `lastMealRecommendTrigger && recommendLoading` | "마지막 끼니 추천을 가져오는 중..." |
| 추천 준비 | `hasRecommendation` | 음식명(bold) + 이유 한 줄 + "식단에 추가" RouterLink 버튼 |

## Row 2 레이아웃 변경

- 추천 카드(`col-span-8`) 제거
- 퀵액션을 3칸 그리드 full-width로 재배치
  - 식단 기록 / 사진 분석 / 오늘 체중 → `grid-cols-3`

## 제거 항목

- `state.recommendLoading` 표시용 별도 카드 UI
- "다시 추천" / "추천 받기" 버튼 (헤더가 좁으므로)
- `loadRecommendation()` 함수 자체는 유지 (trigger 시 자동 호출 로직 그대로)

## 검증

1. `lastMealRecommendTrigger: false` → 헤더에 AI 응원 멘트 표시
2. `lastMealRecommendTrigger: true`, 로딩 중 → 로딩 텍스트 표시
3. 추천 데이터 수신 후 → 음식명 + 이유 + "식단에 추가" 버튼 표시
4. "식단에 추가" 클릭 → `/meals/search?q=음식명` 이동
5. Row 2 퀵액션 3칸 정상 표시
