# 3단계 구현 계획: F302 체중 기록 + F801 칼로리 밸런스 + F802 마지막 끼니 추천

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 체중을 기록하고, 운동 칼로리 소모를 포함한 일일 칼로리 밸런스를 반환하며, 저녁 5시 이후 잔여 칼로리가 1끼 분량일 때 FastAPI AI 마지막 끼니 3개를 추천한다.

**Architecture:** 2단계 브랜치(`feature/F903-F904-routine-session-adjust`)를 베이스로 새 브랜치 생성. `RoutineSession`에 `caloriesBurned` 필드를 추가하여 운동 소모 칼로리를 통합. FastAPI `/ai/meal/last-recommend`는 이미 구현됨 — Spring이 영양 데이터를 모아 프록시 호출만 함.

**Tech Stack:** Spring Boot 3.3 / Java 21 / JPA / MySQL / RestClient / FastAPI (mock 기본)

---

## 브랜치 준비

```powershell
cd C:\kiddo\projects\pjt\yumyum
git checkout feature/F903-F904-routine-session-adjust
git checkout -b feature/F302-F801-F802-weight-calorie-meal
```

---

## 파일 맵

### 신규 (Spring Boot)
| 경로 | 목적 |
|---|---|
| `weight/domain/Weight.java` | 체중 기록 엔티티 |
| `weight/infrastructure/persistence/WeightRepository.java` | JPA 저장소 |
| `weight/application/WeightService.java` | CRUD 서비스 |
| `weight/presentation/CreateWeightRequest.java` | 등록 요청 DTO |
| `weight/presentation/WeightResponse.java` | 응답 DTO |
| `weight/presentation/WeightController.java` | REST 컨트롤러 |
| `nutrition/application/CalorieBalanceService.java` | 밸런스 계산 서비스 |
| `nutrition/presentation/CalorieBalanceController.java` | 밸런스 컨트롤러 |
| `nutrition/presentation/dto/CalorieBalanceResponse.java` | 밸런스 응답 DTO |
| `nutrition/infrastructure/client/AiMealClient.java` | FastAPI 호출 클라이언트 |
| `nutrition/infrastructure/client/AiMealLastRecommendClientRequest.java` | FastAPI 요청 DTO |
| `nutrition/infrastructure/client/AiMealLastRecommendClientResponse.java` | FastAPI 응답 DTO |
| `nutrition/application/AiMealService.java` | 끼니 추천 서비스 |
| `nutrition/presentation/AiMealController.java` | 끼니 추천 컨트롤러 |
| `nutrition/presentation/dto/LastMealRecommendResponse.java` | 클라이언트 응답 DTO |

### 수정 (Spring Boot)
| 경로 | 변경 |
|---|---|
| `routine/domain/RoutineSession.java` | `caloriesBurned` 필드 + create 오버로드 |
| `routine/presentation/CreateSessionRequest.java` | `int caloriesBurned` 필드 추가 |
| `routine/application/RoutineSessionService.java` | caloriesBurned 파라미터 전달 오버로드 |
| `routine/application/RoutineSessionResult.java` | `caloriesBurned` 포함 |
| `routine/presentation/RoutineSessionController.java` | caloriesBurned 전달 |
| `routine/infrastructure/persistence/RoutineSessionRepository.java` | `sumCaloriesBurnedByMemberIdAndDate` 쿼리 추가 |
| `nutrition/infrastructure/persistence/MealRepository.java` | `countByMemberIdAndEffectiveDate` 추가 |
| `global/config/RestClientConfig.java` | `aiMealRestClient` 빈 추가 |

### 테스트
| 경로 | 목적 |
|---|---|
| `weight/domain/WeightTest.java` | 엔티티 단위 테스트 |
| `weight/presentation/WeightControllerTest.java` | 컨트롤러 테스트 |
| `nutrition/application/CalorieBalanceServiceTest.java` | 밸런스 로직 + 트리거 테스트 |
| `nutrition/presentation/AiMealControllerTest.java` | 프록시 컨트롤러 테스트 |

---

## Task 1: F302 — Weight 도메인 + CRUD API

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/weight/domain/Weight.java`
- Create: `backend/src/main/java/com/ssafy/manager/weight/infrastructure/persistence/WeightRepository.java`
- Create: `backend/src/main/java/com/ssafy/manager/weight/application/WeightService.java`
- Create: `backend/src/main/java/com/ssafy/manager/weight/presentation/CreateWeightRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/weight/presentation/WeightResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/weight/presentation/WeightController.java`
- Test: `backend/src/test/java/com/ssafy/manager/weight/domain/WeightTest.java`
- Test: `backend/src/test/java/com/ssafy/manager/weight/presentation/WeightControllerTest.java`

- [ ] **Step 1: 테스트 파일 작성**

```java
// backend/src/test/java/com/ssafy/manager/weight/domain/WeightTest.java
package com.ssafy.manager.weight.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class WeightTest {

    @Test
    void Weight_create_팩토리로_생성된다() {
        Weight w = Weight.create(1L, 72.5, LocalDate.of(2026, 6, 10));

        assertThat(w.getMemberId()).isEqualTo(1L);
        assertThat(w.getWeightKg()).isEqualTo(72.5);
        assertThat(w.getRecordedDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }
}
```

```java
// backend/src/test/java/com/ssafy/manager/weight/presentation/WeightControllerTest.java
package com.ssafy.manager.weight.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.weight.application.WeightService;
import com.ssafy.manager.weight.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeightController.class)
@AutoConfigureMockMvc(addFilters = false)
class WeightControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean WeightService weightService;

    @Test
    void 체중_등록_성공시_201_반환() throws Exception {
        Weight saved = Weight.create(1L, 72.5, LocalDate.of(2026, 6, 10));
        given(weightService.record(any(), any(), any())).willReturn(saved);

        String body = """
                {"memberId": 1, "weightKg": 72.5, "recordedDate": "2026-06-10"}
                """;
        mockMvc.perform(post("/weights").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightKg").value(72.5))
                .andExpect(jsonPath("$.recordedDate").value("2026-06-10"));
    }

    @Test
    void 체중_목록_조회_성공시_200_반환() throws Exception {
        Weight w = Weight.create(1L, 72.5, LocalDate.of(2026, 6, 10));
        given(weightService.findByMember(1L)).willReturn(List.of(w));

        mockMvc.perform(get("/weights").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].weightKg").value(72.5));
    }

    @Test
    void 체중_삭제_성공시_204_반환() throws Exception {
        mockMvc.perform(delete("/weights/1").param("memberId", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 없는_체중_삭제시_404_반환() throws Exception {
        willThrow(new NoSuchElementException("체중 기록을 찾을 수 없습니다."))
                .given(weightService).delete(eq(1L), eq(99L));

        mockMvc.perform(delete("/weights/99").param("memberId", "1"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인**

```powershell
cd backend; ./gradlew test --tests "com.ssafy.manager.weight.*" 2>&1 | Select-Object -Last 10
```

Expected: 컴파일 오류 (Weight 클래스 없음)

- [ ] **Step 3: Weight.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/weight/domain/Weight.java
package com.ssafy.manager.weight.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private double weightKg;
    private LocalDate recordedDate;

    private Weight(Long memberId, double weightKg, LocalDate recordedDate) {
        this.memberId = memberId;
        this.weightKg = weightKg;
        this.recordedDate = recordedDate;
    }

    public static Weight create(Long memberId, double weightKg, LocalDate recordedDate) {
        return new Weight(memberId, weightKg, recordedDate);
    }
}
```

- [ ] **Step 4: WeightRepository.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/weight/infrastructure/persistence/WeightRepository.java
package com.ssafy.manager.weight.infrastructure.persistence;

import com.ssafy.manager.weight.domain.Weight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeightRepository extends JpaRepository<Weight, Long> {
    List<Weight> findByMemberIdOrderByRecordedDateDesc(Long memberId);
}
```

- [ ] **Step 5: WeightService.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/weight/application/WeightService.java
package com.ssafy.manager.weight.application;

import com.ssafy.manager.weight.domain.Weight;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class WeightService {

    private final WeightRepository weightRepository;

    @Transactional
    public Weight record(Long memberId, double weightKg, LocalDate recordedDate) {
        return weightRepository.save(Weight.create(memberId, weightKg, recordedDate));
    }

    @Transactional(readOnly = true)
    public List<Weight> findByMember(Long memberId) {
        return weightRepository.findByMemberIdOrderByRecordedDateDesc(memberId);
    }

    @Transactional
    public void delete(Long memberId, Long weightId) {
        Weight weight = weightRepository.findById(weightId)
                .orElseThrow(() -> new NoSuchElementException("체중 기록을 찾을 수 없습니다."));
        if (!weight.getMemberId().equals(memberId)) {
            throw new NoSuchElementException("체중 기록을 찾을 수 없습니다.");
        }
        weightRepository.delete(weight);
    }
}
```

- [ ] **Step 6: CreateWeightRequest.java + WeightResponse.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/weight/presentation/CreateWeightRequest.java
package com.ssafy.manager.weight.presentation;

import java.time.LocalDate;

public record CreateWeightRequest(Long memberId, double weightKg, LocalDate recordedDate) {}
```

```java
// backend/src/main/java/com/ssafy/manager/weight/presentation/WeightResponse.java
package com.ssafy.manager.weight.presentation;

import com.ssafy.manager.weight.domain.Weight;

import java.time.LocalDate;

public record WeightResponse(Long id, double weightKg, LocalDate recordedDate) {
    public static WeightResponse from(Weight w) {
        return new WeightResponse(w.getId(), w.getWeightKg(), w.getRecordedDate());
    }
}
```

- [ ] **Step 7: WeightController.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/weight/presentation/WeightController.java
package com.ssafy.manager.weight.presentation;

import com.ssafy.manager.weight.application.WeightService;
import com.ssafy.manager.weight.domain.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/weights")
@RequiredArgsConstructor
public class WeightController {

    private final WeightService weightService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WeightResponse record(@RequestBody CreateWeightRequest request) {
        Weight saved = weightService.record(request.memberId(), request.weightKg(), request.recordedDate());
        return WeightResponse.from(saved);
    }

    @GetMapping
    public List<WeightResponse> list(@RequestParam Long memberId) {
        return weightService.findByMember(memberId).stream()
                .map(WeightResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam Long memberId) {
        weightService.delete(memberId, id);
    }
}
```

- [ ] **Step 8: 테스트 실행 — 통과 확인**

```powershell
cd backend; ./gradlew test --tests "com.ssafy.manager.weight.*"
```

Expected: 5 tests passed (WeightTest 1개 + WeightControllerTest 4개)

- [ ] **Step 9: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add backend/src/main/java/com/ssafy/manager/weight/ `
        backend/src/test/java/com/ssafy/manager/weight/
git commit -m "feat(weight): F302 체중 기록 CRUD (POST/GET/DELETE /weights)"
```

---

## Task 2: F801 — RoutineSession.caloriesBurned 추가

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/routine/domain/RoutineSession.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/presentation/CreateSessionRequest.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionResult.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionService.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineSessionController.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineSessionRepository.java`

- [ ] **Step 1: RoutineSession.java에 caloriesBurned 추가**

기존 파일을 아래로 교체한다:

```java
// backend/src/main/java/com/ssafy/manager/routine/domain/RoutineSession.java
package com.ssafy.manager.routine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long routineId;
    private Long memberId;
    private LocalDate sessionDate;
    private LocalDateTime completedAt;
    private int caloriesBurned;

    private RoutineSession(Long routineId, Long memberId, LocalDate sessionDate, int caloriesBurned) {
        this.routineId = routineId;
        this.memberId = memberId;
        this.sessionDate = sessionDate;
        this.completedAt = LocalDateTime.now();
        this.caloriesBurned = caloriesBurned;
    }

    public static RoutineSession create(Long routineId, Long memberId, LocalDate sessionDate) {
        return new RoutineSession(routineId, memberId, sessionDate, 0);
    }

    public static RoutineSession create(Long routineId, Long memberId, LocalDate sessionDate, int caloriesBurned) {
        return new RoutineSession(routineId, memberId, sessionDate, caloriesBurned);
    }
}
```

- [ ] **Step 2: CreateSessionRequest.java에 caloriesBurned 추가**

`int caloriesBurned` 필드를 추가. 기존 JSON에 해당 필드가 없으면 Jackson이 0으로 처리하므로 기존 테스트는 그대로 통과한다.

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/CreateSessionRequest.java
package com.ssafy.manager.routine.presentation;

import java.time.LocalDate;
import java.util.List;

public record CreateSessionRequest(
        Long memberId,
        Long routineId,
        LocalDate sessionDate,
        int caloriesBurned,
        List<SetItem> sets
) {
    public record SetItem(
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {}
}
```

- [ ] **Step 3: RoutineSessionResult.java에 caloriesBurned 추가**

```java
// backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionResult.java
package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RoutineSessionResult(
        Long sessionId,
        Long routineId,
        Long memberId,
        LocalDate sessionDate,
        LocalDateTime completedAt,
        int caloriesBurned,
        List<SetResult> sets
) {
    public record SetResult(
            Long id,
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {
        public static SetResult from(SessionSet s) {
            return new SetResult(s.getId(), s.getExerciseId(), s.getExerciseName(),
                    s.getSetNumber(), s.getActualReps(), s.getActualWeightKg(), s.isCompleted());
        }
    }

    public static RoutineSessionResult from(RoutineSession session, List<SessionSet> sets) {
        return new RoutineSessionResult(
                session.getId(), session.getRoutineId(), session.getMemberId(),
                session.getSessionDate(), session.getCompletedAt(),
                session.getCaloriesBurned(),
                sets.stream().map(SetResult::from).toList()
        );
    }
}
```

- [ ] **Step 4: RoutineSessionService.java에 오버로드 추가**

기존 `recordSession(memberId, routineId, sessionDate, setInputs)` 메서드는 그대로 유지하고(기존 테스트 호환), caloriesBurned를 받는 오버로드를 추가한다.

기존 메서드 시그니처:
```java
public RoutineSessionResult recordSession(Long memberId, Long routineId,
                                           LocalDate sessionDate,
                                           List<SessionSetInput> setInputs) {
```

이 줄에서 내부 `RoutineSession.create(routineId, memberId, sessionDate)` 호출을 찾아서 아래와 같이 교체하고, 오버로드를 추가한다. 전체 수정 후 메서드 두 개:

```java
// 기존 호출자(테스트)를 위해 유지 — caloriesBurned = 0 기본값
@Transactional
public RoutineSessionResult recordSession(Long memberId, Long routineId,
                                           LocalDate sessionDate,
                                           List<SessionSetInput> setInputs) {
    return recordSession(memberId, routineId, sessionDate, 0, setInputs);
}

// 실제 구현
@Transactional
public RoutineSessionResult recordSession(Long memberId, Long routineId,
                                           LocalDate sessionDate,
                                           int caloriesBurned,
                                           List<SessionSetInput> setInputs) {
    if (!routineRepository.existsById(routineId)) {
        throw new NoSuchElementException("루틴을 찾을 수 없습니다.");
    }

    RoutineSession session = RoutineSession.create(routineId, memberId, sessionDate, caloriesBurned);
    routineSessionRepository.save(session);

    List<SessionSet> sets = setInputs.stream()
            .map(i -> SessionSet.create(session.getId(), i.exerciseId(), i.exerciseName(),
                    i.setNumber(), i.actualReps(), i.actualWeightKg(), i.completed()))
            .toList();
    sessionSetRepository.saveAll(sets);

    adjustAndSave(routineId);
    return RoutineSessionResult.from(session, sets);
}
```

`adjustAndSave` 메서드는 그대로 유지한다.

- [ ] **Step 5: RoutineSessionController.java에 caloriesBurned 전달**

컨트롤러의 `recordSession` 메서드에서 `request.caloriesBurned()`를 서비스에 전달한다.

기존 코드:
```java
return SessionResponse.from(routineSessionService.recordSession(
        request.memberId(), request.routineId(), request.sessionDate(), inputs));
```

수정 후:
```java
return SessionResponse.from(routineSessionService.recordSession(
        request.memberId(), request.routineId(), request.sessionDate(),
        request.caloriesBurned(), inputs));
```

- [ ] **Step 6: RoutineSessionRepository.java에 sumCaloriesBurned 쿼리 추가**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineSessionRepository.java
package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoutineSessionRepository extends JpaRepository<RoutineSession, Long> {
    List<RoutineSession> findTop4ByRoutineIdOrderBySessionDateDesc(Long routineId);

    @Query("SELECT COALESCE(SUM(rs.caloriesBurned), 0) FROM RoutineSession rs WHERE rs.memberId = :memberId AND rs.sessionDate = :date")
    int sumCaloriesBurnedByMemberIdAndDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);
}
```

- [ ] **Step 7: 기존 테스트 통과 확인**

```powershell
cd backend; ./gradlew test --tests "com.ssafy.manager.routine.*"
```

Expected: 모든 기존 루틴 테스트 통과. 실패 시 원인 분석 후 수정.

특히 `RoutineSessionControllerTest`에서 기존 JSON body에 `caloriesBurned`가 없어도 int 기본값 0으로 역직렬화되므로 통과해야 한다. 만약 실패 시 test body에 `"caloriesBurned": 0`을 추가한다.

`RoutineSessionServiceTest`는 4-파라미터 `recordSession` 오버로드를 호출하므로 수정 없이 통과한다.

- [ ] **Step 8: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add backend/src/main/java/com/ssafy/manager/routine/domain/RoutineSession.java `
        backend/src/main/java/com/ssafy/manager/routine/presentation/CreateSessionRequest.java `
        backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionResult.java `
        backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionService.java `
        backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineSessionController.java `
        backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineSessionRepository.java
git commit -m "feat(routine): RoutineSession.caloriesBurned 추가 (F801 칼로리 밸런스 기반)"
```

---

## Task 3: F801 — CalorieBalance 엔드포인트

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/application/CalorieBalanceService.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/CalorieBalanceResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/CalorieBalanceController.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/persistence/MealRepository.java`
- Test: `backend/src/test/java/com/ssafy/manager/nutrition/application/CalorieBalanceServiceTest.java`

- [ ] **Step 1: MealRepository에 countByMemberIdAndEffectiveDate 추가**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/persistence/MealRepository.java
package com.ssafy.manager.nutrition.infrastructure.persistence;

import com.ssafy.manager.nutrition.domain.Meal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findAllByMemberIdAndDate(Long memberId, LocalDate date);
    int countByMemberIdAndEffectiveDate(Long memberId, LocalDate effectiveDate);
}
```

- [ ] **Step 2: CalorieBalanceResponse.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/CalorieBalanceResponse.java
package com.ssafy.manager.nutrition.presentation.dto;

public record CalorieBalanceResponse(
        int targetCalories,
        double intakeCalories,
        int burnedCalories,
        double remainingCalories,
        int mealCount,
        boolean lastMealRecommendTrigger
) {}
```

- [ ] **Step 3: 서비스 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/nutrition/application/CalorieBalanceServiceTest.java
package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nutrition.presentation.dto.CalorieBalanceResponse;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CalorieBalanceServiceTest {

    @Mock DailyGoalRepository dailyGoalRepository;
    @Mock MealItemRepository mealItemRepository;
    @Mock MealRepository mealRepository;
    @Mock RoutineSessionRepository routineSessionRepository;

    @InjectMocks CalorieBalanceService calorieBalanceService;

    private static final LocalDate DATE = LocalDate.of(2026, 6, 10);
    private static final Long MEMBER_ID = 1L;

    @Test
    void DailyGoal없으면_0으로_채운_응답_반환() {
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.empty());

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(18, 0));

        assertThat(resp.targetCalories()).isEqualTo(0);
        assertThat(resp.lastMealRecommendTrigger()).isFalse();
    }

    @Test
    void 운동_소모_칼로리가_잔여칼로리에_반영된다() {
        DailyGoal goal = DailyGoal.of(MEMBER_ID, DATE, 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.of(goal));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(1500.0);
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(300);
        given(mealRepository.countByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(2);

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(16, 0));

        // remaining = 2000 + 300 - 1500 = 800
        assertThat(resp.remainingCalories()).isEqualTo(800.0);
        assertThat(resp.burnedCalories()).isEqualTo(300);
    }

    @Test
    void 트리거_조건_충족시_lastMealRecommendTrigger_true() {
        // target=2000, intake=1333, burned=0, mealCount=3
        // oneMealTarget = 2000/3 ≈ 667
        // remaining = 2000 - 1333 = 667 → 667 * 0.8=533, 667 * 1.2=800 → 667 in range → trigger=true
        DailyGoal goal = DailyGoal.of(MEMBER_ID, DATE, 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.of(goal));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(1333.0);
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(0);
        given(mealRepository.countByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(3);

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(18, 0));

        assertThat(resp.lastMealRecommendTrigger()).isTrue();
    }

    @Test
    void 오후5시_미만이면_트리거_false() {
        DailyGoal goal = DailyGoal.of(MEMBER_ID, DATE, 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.of(goal));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(1333.0);
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(0);
        given(mealRepository.countByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(3);

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(14, 0));

        assertThat(resp.lastMealRecommendTrigger()).isFalse();
    }
}
```

- [ ] **Step 4: 테스트 실행 — 컴파일 실패 확인**

```powershell
cd backend; ./gradlew test --tests "com.ssafy.manager.nutrition.application.CalorieBalanceServiceTest" 2>&1 | Select-Object -Last 10
```

Expected: 컴파일 오류 (CalorieBalanceService 없음)

- [ ] **Step 5: CalorieBalanceService.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/application/CalorieBalanceService.java
package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nutrition.presentation.dto.CalorieBalanceResponse;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CalorieBalanceService {

    private final DailyGoalRepository dailyGoalRepository;
    private final MealItemRepository mealItemRepository;
    private final MealRepository mealRepository;
    private final RoutineSessionRepository routineSessionRepository;

    @Transactional(readOnly = true)
    public CalorieBalanceResponse getBalance(Long memberId, LocalDate date, LocalTime currentTime) {
        DailyGoal goal = dailyGoalRepository.findByMemberIdAndDate(memberId, date).orElse(null);
        if (goal == null) {
            return new CalorieBalanceResponse(0, 0.0, 0, 0.0, 0, false);
        }

        double intakeCalories = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(memberId, date);
        int burnedCalories = routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(memberId, date);
        int mealCount = mealRepository.countByMemberIdAndEffectiveDate(memberId, date);

        int targetCalories = (int) goal.getTargetValue();
        double remainingCalories = targetCalories + burnedCalories - intakeCalories;

        boolean trigger = false;
        if (currentTime.getHour() >= 17 && mealCount >= 1) {
            double oneMealTarget = (double) targetCalories / mealCount;
            trigger = remainingCalories >= oneMealTarget * 0.8
                   && remainingCalories <= oneMealTarget * 1.2;
        }

        return new CalorieBalanceResponse(
                targetCalories, intakeCalories, burnedCalories,
                remainingCalories, mealCount, trigger
        );
    }
}
```

- [ ] **Step 6: CalorieBalanceController.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/presentation/CalorieBalanceController.java
package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.CalorieBalanceService;
import com.ssafy.manager.nutrition.presentation.dto.CalorieBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/calorie-balance")
@RequiredArgsConstructor
public class CalorieBalanceController {

    private final CalorieBalanceService calorieBalanceService;

    @GetMapping
    public CalorieBalanceResponse getBalance(
            @RequestParam Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return calorieBalanceService.getBalance(memberId, targetDate, LocalTime.now());
    }
}
```

- [ ] **Step 7: 테스트 실행 — 통과 확인**

```powershell
cd backend; ./gradlew test --tests "com.ssafy.manager.nutrition.application.CalorieBalanceServiceTest"
```

Expected: 4 tests passed

- [ ] **Step 8: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add backend/src/main/java/com/ssafy/manager/nutrition/application/CalorieBalanceService.java `
        backend/src/main/java/com/ssafy/manager/nutrition/presentation/CalorieBalanceController.java `
        backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/CalorieBalanceResponse.java `
        backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/persistence/MealRepository.java `
        backend/src/test/java/com/ssafy/manager/nutrition/application/CalorieBalanceServiceTest.java
git commit -m "feat(nutrition): F801 GET /calorie-balance (운동 소모 포함 + F802 트리거 플래그)"
```

---

## Task 4: F802 — Spring → FastAPI 끼니 추천 프록시

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealClient.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealLastRecommendClientRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealLastRecommendClientResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealService.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/AiMealController.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/LastMealRecommendResponse.java`
- Test: `backend/src/test/java/com/ssafy/manager/nutrition/presentation/AiMealControllerTest.java`

- [ ] **Step 1: 컨트롤러 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/nutrition/presentation/AiMealControllerTest.java
package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.AiMealService;
import com.ssafy.manager.nutrition.presentation.dto.LastMealRecommendResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiMealController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiMealControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AiMealService aiMealService;

    private static final LastMealRecommendResponse RESULT = new LastMealRecommendResponse(
            List.of(new LastMealRecommendResponse.MealRecommendation(
                    "닭가슴살 샐러드", 380.0, 42.0, 18.0, 12.0, "단백질 보충")),
            "protein",
            "단백질이 부족합니다. 아래 식단으로 마무리하세요!"
    );

    @Test
    void 끼니_추천_성공시_200과_추천_목록_반환() throws Exception {
        given(aiMealService.lastRecommend(any(), any())).willReturn(RESULT);

        mockMvc.perform(post("/ai/meals/last-recommend").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].name").value("닭가슴살 샐러드"))
                .andExpect(jsonPath("$.priorityNutrient").value("protein"))
                .andExpect(jsonPath("$.aiComment").isNotEmpty());
    }

    @Test
    void FastAPI_장애시_503_반환() throws Exception {
        given(aiMealService.lastRecommend(any(), any()))
                .willThrow(new RestClientException("FastAPI 연결 실패"));

        mockMvc.perform(post("/ai/meals/last-recommend").param("memberId", "1"))
                .andExpect(status().isServiceUnavailable());
    }
}
```

- [ ] **Step 2: RestClientConfig.java에 aiMealRestClient 빈 추가**

기존 파일에 아래 빈을 추가한다:

```java
@Bean
RestClient aiMealRestClient(
        @Value("${ai.fastapi.url}") String baseUrl) {
    return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
}
```

- [ ] **Step 3: AiMealLastRecommendClientRequest.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealLastRecommendClientRequest.java
package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiMealLastRecommendClientRequest(
        @JsonProperty("total_kcal")      double totalKcal,
        @JsonProperty("total_protein_g") double totalProteinG,
        @JsonProperty("total_carb_g")    double totalCarbG,
        @JsonProperty("total_fat_g")     double totalFatG,
        @JsonProperty("target_kcal")      double targetKcal,
        @JsonProperty("target_protein_g") double targetProteinG,
        @JsonProperty("target_carb_g")    double targetCarbG,
        @JsonProperty("target_fat_g")     double targetFatG,
        @JsonProperty("meal_count")       int mealCount
) {}
```

- [ ] **Step 4: AiMealLastRecommendClientResponse.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealLastRecommendClientResponse.java
package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiMealLastRecommendClientResponse(
        @JsonProperty("recommendations")   List<Recommendation> recommendations,
        @JsonProperty("priority_nutrient") String priorityNutrient,
        @JsonProperty("ai_comment")        String aiComment
) {
    public record Recommendation(
            @JsonProperty("name")      String name,
            @JsonProperty("kcal")      double kcal,
            @JsonProperty("protein_g") double proteinG,
            @JsonProperty("carb_g")    double carbG,
            @JsonProperty("fat_g")     double fatG,
            @JsonProperty("reason")    String reason
    ) {}
}
```

- [ ] **Step 5: AiMealClient.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealClient.java
package com.ssafy.manager.nutrition.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiMealClient {

    private final RestClient aiMealRestClient;

    public AiMealLastRecommendClientResponse lastRecommend(AiMealLastRecommendClientRequest request) {
        return aiMealRestClient.post()
                .uri("/ai/meal/last-recommend")
                .body(request)
                .retrieve()
                .body(AiMealLastRecommendClientResponse.class);
    }
}
```

- [ ] **Step 6: LastMealRecommendResponse.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/LastMealRecommendResponse.java
package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientResponse;

import java.util.List;

public record LastMealRecommendResponse(
        List<MealRecommendation> recommendations,
        String priorityNutrient,
        String aiComment
) {
    public record MealRecommendation(
            String name, double kcal, double proteinG, double carbG, double fatG, String reason
    ) {}

    public static LastMealRecommendResponse from(AiMealLastRecommendClientResponse resp) {
        List<MealRecommendation> recs = resp.recommendations().stream()
                .map(r -> new MealRecommendation(r.name(), r.kcal(), r.proteinG(), r.carbG(), r.fatG(), r.reason()))
                .toList();
        return new LastMealRecommendResponse(recs, resp.priorityNutrient(), resp.aiComment());
    }
}
```

- [ ] **Step 7: AiMealService.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealService.java
package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.domain.MealItem;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealClient;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientRequest;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientResponse;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nutrition.presentation.dto.LastMealRecommendResponse;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AiMealService {

    private final MealItemRepository mealItemRepository;
    private final MealRepository mealRepository;
    private final ProgramRepository programRepository;
    private final AiMealClient aiMealClient;

    @Transactional(readOnly = true)
    public LastMealRecommendResponse lastRecommend(Long memberId, LocalDate effectiveDate) {
        Program program = programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("활성 프로그램이 없습니다."));

        List<MealItem> items = mealItemRepository.findAllByMemberIdAndEffectiveDate(memberId, effectiveDate);
        double totalKcal    = items.stream().mapToDouble(MealItem::getCalories).sum();
        double totalProtein = items.stream().mapToDouble(MealItem::getProtein).sum();
        double totalCarb    = items.stream().mapToDouble(MealItem::getCarbs).sum();
        double totalFat     = items.stream().mapToDouble(MealItem::getFat).sum();
        int mealCount = mealRepository.countByMemberIdAndEffectiveDate(memberId, effectiveDate);

        AiMealLastRecommendClientResponse resp = aiMealClient.lastRecommend(
                new AiMealLastRecommendClientRequest(
                        totalKcal, totalProtein, totalCarb, totalFat,
                        program.getTargetCalories(), program.getTargetProteinG(),
                        program.getTargetCarbG(), program.getTargetFatG(),
                        mealCount
                )
        );
        return LastMealRecommendResponse.from(resp);
    }
}
```

- [ ] **Step 8: AiMealController.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/nutrition/presentation/AiMealController.java
package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.AiMealService;
import com.ssafy.manager.nutrition.presentation.dto.LastMealRecommendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/ai/meals")
@RequiredArgsConstructor
public class AiMealController {

    private final AiMealService aiMealService;

    @PostMapping("/last-recommend")
    public LastMealRecommendResponse lastRecommend(@RequestParam Long memberId) {
        return aiMealService.lastRecommend(memberId, LocalDate.now());
    }
}
```

- [ ] **Step 9: 테스트 실행 — 통과 확인**

```powershell
cd backend; ./gradlew test --tests "com.ssafy.manager.nutrition.presentation.AiMealControllerTest"
```

Expected: 2 tests passed

- [ ] **Step 10: 전체 테스트 실행**

```powershell
cd backend; ./gradlew test
```

Expected: 전체 통과. 사전에 존재하던 `ManagerApplicationTests.contextLoads()` DB 접속 실패는 무시 (MySQL 미실행 환경).

- [ ] **Step 11: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java `
        backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/ `
        backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealService.java `
        backend/src/main/java/com/ssafy/manager/nutrition/presentation/AiMealController.java `
        backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/LastMealRecommendResponse.java `
        backend/src/test/java/com/ssafy/manager/nutrition/presentation/AiMealControllerTest.java
git commit -m "feat(nutrition): F802 POST /ai/meals/last-recommend (FastAPI 끼니 추천 프록시)"
```

---

## 완료 체크리스트

- [ ] `POST /weights` → 201, 체중 저장
- [ ] `GET /weights?memberId=` → 200, 날짜 내림차순 목록
- [ ] `DELETE /weights/{id}?memberId=` → 204, 타인 기록은 404
- [ ] `POST /routines/sessions` body에 `caloriesBurned` 포함 시 저장됨
- [ ] `GET /calorie-balance?memberId=` → burnedCalories 반영된 remainingCalories 반환
- [ ] 17시 이후 + mealCount≥1 + remaining 범위 내 → `lastMealRecommendTrigger: true`
- [ ] `POST /ai/meals/last-recommend?memberId=` → 추천 3개 반환
- [ ] FastAPI 장애 → 503 반환
- [ ] 기존 루틴 테스트 전체 통과
