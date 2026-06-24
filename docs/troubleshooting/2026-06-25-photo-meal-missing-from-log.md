# 사진으로 기록한 식단이 식단 기록 탭에서 안 보임
**Date:** 2026-06-25
**작성자:** g0rnn
**Time spent:** ~20분

## Symptom

`POST /meals/photo`로 식단을 기록하면 DB `meal` 테이블에는 정상적으로 행이 생기지만,
식단 기록 탭(`GET /meals?date=...`) 응답에는 해당 식단이 나타나지 않는다.

```
mysql> select * from meal;
+------------+----------------+----+-----------+----------------------------+--------+-------+
| date       | effective_date | id | member_id | recorded_at                | source | type  |
+------------+----------------+----+-----------+----------------------------+--------+-------+
| 2026-06-24 | 2026-06-24     |  1 |         1 | 2026-06-25 00:58:41.304992 | MANUAL | LUNCH |
| 2026-06-24 | 2026-06-24     |  2 |         1 | 2026-06-25 00:58:54.761025 | MANUAL | LUNCH |
| 2026-06-25 | 2026-06-24     |  3 |         1 | 2026-06-25 01:00:03.373365 | PHOTO  | LUNCH |
+------------+----------------+----+-----------+----------------------------+--------+-------+
```

## Wrong tracks

- 처음엔 프론트 `listMeals(date)` 호출 파라미터가 잘못된 게 아닌지 의심했다.
- DB를 직접 조회해 같은 회원의 `MANUAL` 식사 두 건(`id=1,2`)은 정상 노출되고 `PHOTO` 식사 한 건(`id=3`)만 빠지는 것을 확인하면서, 프론트가 아니라 기록 경로(`record()` vs `recordFromPhoto()`) 차이 쪽으로 좁혀졌다.

## Root cause

`meal` 테이블에는 `date`와 `effective_date` 두 컬럼이 있고, `MealController.list()` → `MealService.listByDate()`는 **`date` 컬럼**으로 필터링한다(`mealRepository.findAllByMemberIdAndDate`).

- `record()`(수동 기록)는 프론트가 보낸 날짜(`command.date()`)를 `date`에 저장한다. 이 값은 이번 effective-date 수정으로 `getEffectiveToday()`를 거치므로 `effective_date`와 항상 같다.
- `recordFromPhoto()`는 `date`를 `recordedAt.toLocalDate()`(보정 없는 실제 날짜)로 저장했다. 자정~새벽 4시 사이에 사진을 기록하면 `date=다음날`, `effective_date=전날`로 어긋나, `date` 기준 목록 조회에서 빠졌다.

위 예시의 `id=3` 행이 정확히 이 케이스다 (`recorded_at`이 00:58, `date=2026-06-25`, `effective_date=2026-06-24`).

## Fix

`MealService.recordFromPhoto()`에서 `Meal` 생성 시 `date` 인자를 `recordedAt.toLocalDate()` → `effectiveDate`로 변경해 `record()`와 동일하게 통일했다.

```java
// Before
Meal meal = new Meal(command.memberId(), resolvedType, recordedAt.toLocalDate(), effectiveDate,
        MealSource.PHOTO, recordedAt);

// After
Meal meal = new Meal(command.memberId(), resolvedType, effectiveDate, effectiveDate,
        MealSource.PHOTO, recordedAt);
```

기존에 이미 잘못 저장된 행(`id=3`)은 코드 수정만으로는 고쳐지지 않으므로, DB에서 `date` 값을 `effective_date`와 동일하게 직접 보정해야 한다.

## Prevention

- 같은 엔티티에 의미가 겹치는 두 날짜 필드(`date`, `effectiveDate`)를 둘 때는, 모든 생성 경로(`record`, `recordFromPhoto` 등)가 동일한 값을 넣는지 테스트로 강제한다.
- 두 필드가 항상 같은 값이어야 한다면, `date` 필드를 없애고 `effectiveDate` 하나로 통합하는 리팩토링을 검토한다.
