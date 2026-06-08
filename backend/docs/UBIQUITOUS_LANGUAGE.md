# 냠냠코치 — Ubiquitous Language

> 코드·문서·대화에서 반드시 이 용어를 사용한다.
> 확정 용어 이외의 동의어·유사어는 ❌ 열에 명시하며, 혼용을 금지한다.

---

## 핵심 도메인 용어

| 영문 (코드 기준) | 한국어 | 한 줄 정의 | 사용 금지 |
|---|---|---|---|
| **Member** | 회원 | 냠냠코치에 가입하여 식단·목표를 관리하는 앱 사용자 | ~~User~~(기술 레이어 전용), ~~사용자~~ |
| **HealthGoal** | 건강 목표 | Member가 온보딩 시 선택하는 앱 전체 방향 (`DIET` / `MUSCLE` / `HEALTH` / `DISEASE`) | ~~goal~~(범위 불명확), ~~목표~~(단독 사용) |
| **Food** | 음식 | 영양 정보 마스터 데이터의 단위 (100g당 칼로리·탄수화물·단백질·지방·식이섬유) | ~~FoodDB~~, ~~FoodItem~~ |
| **Meal** | 식단 | Member가 한 끼 식사를 기록한 결과물 | ~~MealRecord~~, ~~식단 기록~~ |
| **MealItem** | 식단 항목 | Meal에 포함된 개별 음식과 섭취량(g)의 쌍. Food를 참조한다 | ~~meal_items~~(기술 레이어) |
| **Program** | 프로그램 | Member가 기간·목표를 설정하면 AI가 생성하는 구조화된 건강 관리 과정. 시작일~종료일이 정해진다 | ~~Plan~~, ~~WeeklyPlan~~ |
| **WeeklyReport** | 주간 리포트 | Program 진행 중 매 7일마다 AI가 생성하는 식단 분석 피드백 | ~~ai_report~~, ~~주간 플랜~~ |
| **DailyGoal** | 일일 목표 | Member가 하루 단위로 달성해야 하는 숫자 기반 목표. 달성/미달성 두 상태를 가지며, 1차 MVP는 칼로리(kcal) 기준. Program 없으면 성별 기본값(남성 2400 / 여성 1800 kcal), Program 있으면 TDEE+타입 조정값 적용 | ~~일일 미션~~, ~~daily_goals~~ |
| **Streak** | 연속 달성 | DailyGoal을 연속으로 달성한 일수. 하루라도 미달성 시 0으로 리셋 | ~~연속 달성일~~, ~~스트릭~~(코드에서) |
| **Nyam** | 냠냠이 | Member의 목표 달성률에 따라 외형이 변화하는 마스코트 캐릭터 (2차 MVP: 외형·메시지, 3차 MVP: 스킨 해금) | ~~캐릭터~~, ~~마스코트~~ |
| **Routine** | 운동 루틴 | Member가 등록한 운동 계획. `USER_INPUT`(직접 입력) 또는 `AI_GENERATED`(AI 추천) 두 가지 출처를 가진다. 회원당 ACTIVE 루틴은 1개만 허용 | ~~운동 계획~~, ~~workout~~ |
| **RoutineExercise** | 루틴 운동 항목 | Routine에 포함된 개별 운동과 계획 세트·무게·반복 수의 묶음. `weekNumber`로 주차별 AI 조정 버전을 관리한다 | ~~exercise~~(단독 사용), ~~운동 항목~~ |
| **RoutineSession** | 루틴 세션 | Member가 특정 날 Routine을 실제 수행한 기록. 명시적 시작(`POST /routines/sessions`)과 완료(`PATCH .../complete`)로 생명주기를 관리한다 | ~~운동 기록~~, ~~workout session~~ |
| **SessionSet** | 세션 세트 | RoutineSession 내 세트 단위 기록. 실제 무게·반복 수와 성공/실패 여부를 저장한다 | ~~set~~(단독 사용) |
| **RoutineSessionFeedback** | 루틴 세션 피드백 | RoutineSession 완료 후 AI(`/ai/routine/adjust`)가 반환하는 조정 결과. `UP/HOLD/DOWN/DELOAD` 방향과 세부 조정 내용을 JSON으로 저장한다 | ~~adjustment~~, ~~AI 피드백~~ |

---

## 관계 요약

```
Member
 ├── HealthGoal          (온보딩 시 1개 선택)
 ├── Meal 0..*           (Program 없이도 기록 가능)
 │    └── MealItem 1..*
 │         └── Food      (영양 정보 참조)
 ├── Program 0..*        (기간+목표 기반 AI 생성 계획)
 │    └── WeeklyReport 0..* (매 7일마다 생성)
 ├── DailyGoal 0..*      (Program과 독립적으로 존재)
 │    ※ Program 활성 시 Program이 target을 결정
 │    ※ Program 없으면 시스템 기본값(하루 2끼) 적용
 ├── Streak              (DailyGoal 연속 달성 카운트)
 │    ├── current_streak
 │    └── max_streak
 └── Routine 0..*        (운동 루틴, ACTIVE 1개만 허용)
      ├── RoutineExercise 1..* (weekNumber별 운동 항목)
      └── RoutineSession 0..*  (실제 수행 기록)
           ├── SessionSet 0..*
           └── RoutineSessionFeedback 0..1 (AI 조정 결과)
```

---

## MVP 범위별 용어 구현 상태

| 용어 | 1차 MVP | 2차 MVP | 3차 MVP |
|---|---|---|---|
| Member | ✅ | — | — |
| HealthGoal | ✅ | — | — |
| Food | ✅ | — | — |
| Meal / MealItem | ✅ | — | — |
| Program / WeeklyReport | ✅ | — | — |
| DailyGoal | ✅ | — | — |
| Streak | ✅ | — | — |
| Routine / RoutineExercise | — | ✅ | — |
| RoutineSession / SessionSet | — | ✅ | — |
| RoutineSessionFeedback | — | ✅ | — |
| Nyam (외형·메시지) | — | ✅ | — |
| Nyam (스킨 해금) | — | — | ✅ |
| Level / Badge | — | — | ✅ |
