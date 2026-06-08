# 냠냠코치 — Bounded Context Glossary

> 이 파일은 도메인 용어 사전입니다. 구현 세부사항(쿼리, 클래스명, 알고리즘)은 기록하지 않습니다.
> 용어가 확정될 때마다 즉시 업데이트합니다.

---

## Program (프로그램)

Member가 기간과 목표를 설정하면 AI가 생성하는 구조화된 건강 관리 과정. 시작일과 종료일이 정해져 있다.

- Member는 목표(예: 다이어트)와 기간(예: 2개월)을 제공한다.
- AI는 목표와 기간을 바탕으로 Program을 수립한다.
- Program이 종료되면 Member는 새로운 Program을 시작할 수 있다.
- Program 진행 중 매 7일마다 **WeeklyReport**가 생성된다.
- **Program**이 아닌 용어: ~~Plan~~(범위 불분명), ~~WeeklyPlan~~(기간 단위를 잘못 표현)

## WeeklyReport (주간 리포트)

Program 진행 중 매 7일마다 AI가 생성하는 식단 분석 피드백 결과물.

- WeeklyReport는 Program에 종속된다.
- **WeeklyReport**가 아닌 용어: ~~ai_report~~(기술 레이어 필드명), ~~주간 플랜~~

## DailyGoal (일일 목표)

Member가 하루 단위로 달성해야 하는 목표. 달성(`achieved`) 또는 미달성(`not achieved`) 두 상태를 가진다.

- DailyGoal은 Program에 종속되지 않는다. Program 없이도 존재하며 Streak에 기여한다.
- 달성 기준(targetValue): 칼로리(kcal) 기반. Program이 없으면 Member.sex 기반 기본값(남성 2400 kcal / 여성 1800 kcal, 온보딩 완료 시 TDEE로 대체)을 따른다. Program이 활성이면 TDEE를 Program 타입(MUSCLE/DIET/HEALTH)에 따라 조정한 값을 적용한다.
- DailyGoal의 달성 여부가 **Streak** 카운트의 기준이 된다.
- **DailyGoal**이 아닌 용어: ~~일일 미션~~(게이미피케이션 용어와 혼동), ~~daily_goals~~(기술 레이어 테이블명)

## Streak (연속 달성)

Member가 DailyGoal을 연속으로 달성한 일수. 하루라도 달성하지 못하면 0으로 리셋된다.

- `current_streak`: 현재 연속 달성일
- `max_streak`: 역대 최고 Streak 기록
- Streak이 끊길 위험이 있을 때 냠냠이가 Member에게 푸시 알림을 보낸다.
- **Streak**이 아닌 용어: ~~연속 달성일~~(비공식), ~~스트릭~~(한글 표기는 혼용 가능하나 코드에서는 Streak으로 통일)

## Nyam (냠냠이)

냠냠코치의 마스코트 캐릭터. Member의 목표 달성률에 따라 외형이 변화하고, 상황에 맞는 메시지를 출력한다.

- Nyam은 DB에 저장되지 않는 **stateless 계산 값**이다. 요청마다 `DailyGoal` 달성률과 `Member.healthGoal`을 조합해 실시간 계산한다.
- **외형 단계 (AppearanceStage)**: 달성률 기준 4단계 (0~30% / 30~60% / 60~90% / 90~100%)
- **반응 메시지 상황 (NyamMessageType)**:
  - `GOAL_ACHIEVED` — 오늘 DailyGoal 달성
  - `STREAK_MILESTONE` — Streak 3·7·30일 도달
  - `LAST_MEAL_REMIND` — 마지막 끼니 추천 조건 충족
  - `DEFAULT` — 그 외 기본 메시지
- 2차 MVP에서 외형·메시지 구현. 3차 MVP에서 스킨 해금(G103) 추가.
- **Nyam**이 아닌 용어: ~~캐릭터~~(일반 명사), ~~마스코트~~(비공식)

## HealthGoal (건강 목표)

Member가 온보딩 시 선택하는 앱 전체 방향을 결정하는 상위 목표. `DIET`(다이어트) / `MUSCLE`(근육 증가) / `HEALTH`(건강 유지) / `DISEASE`(질환 관리) 중 하나다.

- HealthGoal은 목표 칼로리 자동 계산, Program 생성, Nyam 외형 변화 모두에 영향을 미친다.
- **HealthGoal**이 아닌 용어: ~~goal~~(DailyGoal·Program의 목표와 혼동), ~~목표~~(단독 사용 시 범위 불명확)

---

## Food (음식)

식품안전처(MFDS) 또는 자체 입력으로 구성된 영양 정보 마스터 데이터의 단위. 100g당 칼로리·탄수화물·단백질·지방·식이섬유 값을 가진다.

- Food는 삭제되지 않는 참조 데이터다. MealItem이 Food를 참조한다.
- `source` 속성으로 데이터 출처(`MFDS` / `MANUAL` / `AI`)를 구분하여 신뢰도를 관리한다.
- **Food**가 아닌 용어: ~~FoodDB~~, ~~FoodItem~~, ~~음식 항목~~

---

## Meal (식단)

Member가 한 끼 식사를 기록한 결과물. 식사 날짜(`meal_date`)와 식사 구분(`meal_type`: 아침·점심·저녁·간식)을 가진다.

- Meal은 하나 이상의 **MealItem**으로 구성된다.
- **MealItem(식단 항목)**: Meal에 포함된 개별 음식과 섭취량(g)의 쌍. MealItem은 Food를 참조한다.
- **Member**가 아닌 용어: ~~MealRecord~~(행위와 결과물을 혼동), ~~식단 기록~~(비공식)

---

## Member (회원)

냠냠코치에 가입하여 식단·목표를 관리하는 앱 사용자.

- 기술 레이어(인증, 세션)에서는 `User`를 쓸 수 있으나, 비즈니스 레이어의 공식 용어는 **Member**다.
- Member는 `ACTIVE` / `INACTIVE` 두 가지 상태를 가진다. 탈퇴 요청 시 즉시 삭제하지 않고 `INACTIVE`로 전환한다.
- `healthGoal` 필드를 가진다 (`DIET` / `MUSCLE` / `HEALTH` / `DISEASE`). 온보딩 시 선택하며, Program과 독립적으로 Nyam 외형에 영향을 준다.
- **Member**가 아닌 용어: ~~User~~(기술 레이어 전용), ~~사용자~~(비공식)

---

## Routine (운동 루틴)

Member가 등록한 운동 계획. 직접 입력하거나 AI가 생성한다.

- `source` 속성으로 출처를 구분한다: `USER_INPUT`(직접 입력) / `AI_GENERATED`(AI 추천).
- `status`: `ACTIVE` / `INACTIVE`. 회원당 ACTIVE 루틴은 1개만 허용. 새 루틴 등록 시 기존 ACTIVE → INACTIVE 자동 전환.
- `startDate` 기준 경과 주차로 현재 주차를 계산한다: `WEEKS.between(startDate, today) + 1`.
- `AI_GENERATED` 루틴은 비동기로 생성된다. 생성 완료 전에는 `RoutineExercise`가 빈 배열로 반환된다.
- **Routine**이 아닌 용어: ~~운동 계획~~, ~~workout~~

## RoutineExercise (루틴 운동 항목)

Routine에 포함된 개별 운동과 계획 세트·무게·반복 수의 묶음.

- `weekNumber` 컬럼으로 주차별 AI 조정 버전을 관리한다. 동일 운동이라도 주차마다 별도 레코드로 존재한다.
- 해당 주차 `RoutineExercise`가 없으면 직전 주차 레코드를 폴백으로 사용한다.
- AI 조정 결과 수신 시 다음 주차 레코드가 신규 생성된다 (기존 레코드 수정 없음).
- **RoutineExercise**가 아닌 용어: ~~exercise~~(단독 사용)

## RoutineSession (루틴 세션)

Member가 특정 날 Routine을 실제 수행한 기록.

- 명시적 시작(`POST /routines/sessions`)과 완료(`PATCH .../complete`)로 생명주기를 관리한다. 운동은 식단과 달리 정해진 시간에 기록되므로 시작·끝이 명시적이어야 한다.
- 세션 완료 시 비동기로 FastAPI `/ai/routine/adjust`를 호출하고, 결과는 `RoutineSessionFeedback`에 저장된다.
- **RoutineSession**이 아닌 용어: ~~운동 기록~~, ~~workout session~~

## SessionSet (세션 세트)

RoutineSession 내 세트 단위 기록.

- 실제 무게·반복 수와 성공/실패 여부를 저장한다.
- **SessionSet**이 아닌 용어: ~~set~~(단독 사용)

## RoutineSessionFeedback (루틴 세션 피드백)

RoutineSession 완료 후 AI가 반환하는 조정 결과.

- `session_id` (plain FK, JPA 관계 없음) + `adjustment_result` (MySQL JSON 컬럼) 구조.
- `adjustment_result`는 `UP` / `HOLD` / `DOWN` / `DELOAD` 방향과 세부 조정 내용을 포함한다.
- **RoutineSessionFeedback**이 아닌 용어: ~~adjustment~~, ~~AI 피드백~~

