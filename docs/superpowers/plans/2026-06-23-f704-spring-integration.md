# F704 Spring 연동 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DailyBatchJob이 주간 WeeklyReport stub 생성 후 FastAPI `/ai/coaching/weekly`를 호출해 AI 코칭 결과를 저장하고, `GET /programs/{id}/weekly-reports/{week}`로 조회할 수 있게 한다.

**Architecture:** 기존 `WeeklyReportService.createStubs()` 안에서 stub 신규 생성 직후 `WeeklyCoachingDataService`로 7일 데이터를 수집하고 `AiCoachingClient`로 FastAPI를 호출한다. 실패 시 로그만 남기고 stub(content=null)으로 유지한다. WeeklyReport 엔티티에 6개 필드를 추가하고 `fill()` 메서드로 한 번에 채운다.

**Tech Stack:** Spring Boot 3.3 / Java 21 / JPA / MockMvc / Mockito / RestClient

## Global Constraints

- 패키지: `com.ssafy.manager.{domain}.{layer}`
- DTO 접미사: presentation → `Response`/`Request`, application → `Result`/`Command`, infrastructure/client → `ClientRequest`/`ClientResponse`
- 컨트롤러는 항상 `ResponseEntity`로 응답
- 도메인 간 entity 참조는 plain `Long` ID만 허용
- snake_case JSON 직렬화: `@JsonProperty` 사용 (기존 `AiPlanClientRequest` 패턴 동일)
- AI 클라이언트 실패는 예외 전파 없이 로그만 남기고 stub 유지
- `@Slf4j` + Lombok 사용

---

## File Map

| 파일 | 작업 |
|---|---|
| `program/domain/WeeklyReport.java` | 6개 필드 추가 + `fill()` 메서드 |
| `program/infrastructure/persistence/WeeklyReportRepository.java` | `findByProgramIdAndWeekNumber()` 추가 |
| `nutrition/infrastructure/persistence/MealItemRepository.java` | `DailyNutritionSummary` 인터페이스 + 범위 쿼리 추가 |
| `routine/infrastructure/persistence/RoutineSessionRepository.java` | `findByMemberIdAndSessionDateBetween()` 추가 |
| `weight/infrastructure/persistence/WeightRepository.java` | `findByMemberIdAndRecordedDateBetween()` 추가 |
| `program/infrastructure/client/AiCoachingClientRequest.java` | 신규 — FastAPI 요청 DTO (nested records) |
| `program/infrastructure/client/AiCoachingClientResponse.java` | 신규 — FastAPI 응답 DTO |
| `program/infrastructure/client/AiCoachingClient.java` | 신규 — `POST /ai/coaching/weekly` RestClient 호출 |
| `global/config/RestClientConfig.java` | `aiCoachingRestClient` bean 추가 (60s timeout) |
| `program/application/WeeklyCoachingDataService.java` | 신규 — 7일 데이터 수집 → AiCoachingClientRequest 조립 |
| `program/application/WeeklyReportService.java` | stub 생성 후 AI 호출 로직 추가 |
| `program/presentation/dto/WeeklyReportResponse.java` | 신규 — 응답 DTO |
| `program/presentation/WeeklyReportController.java` | 신규 — `GET /programs/{id}/weekly-reports/{week}` |

---

### Task 1: WeeklyReport 엔티티 확장 + Repository 쿼리

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/program/domain/WeeklyReport.java`
- Modify: `backend/src/main/java/com/ssafy/manager/program/infrastructure/persistence/WeeklyReportRepository.java`

**Interfaces:**
- Produces: `WeeklyReport.fill(String content, String nutritionSummary, String exerciseSummary, String goalSummary, double avgCalorieRate, int achievementDays, Double weightTrend)`
- Produces: `WeeklyReportRepository.findByProgramIdAndWeekNumber(Long, int): Optional<WeeklyReport>`
- Produces: getters — `getContent()`, `getNutritionSummary()`, `getExerciseSummary()`, `getGoalSummary()`, `getAvgCalorieRate()`, `getAchievementDays()`, `getWeightTrend()`

- [ ] **Step 1: WeeklyReport.java에 필드 + fill() 추가**

```java
package com.ssafy.manager.program.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long programId;
    private int weekNumber;

    @Column(length = 2000)
    private String content;

    @Column(length = 1000)
    private String nutritionSummary;

    @Column(length = 1000)
    private String exerciseSummary;

    @Column(length = 1000)
    private String goalSummary;

    private double avgCalorieRate;
    private int achievementDays;
    private Double weightTrend;

    public WeeklyReport(Long programId, int weekNumber) {
        this.programId = programId;
        this.weekNumber = weekNumber;
    }

    public void fill(String content, String nutritionSummary, String exerciseSummary,
                     String goalSummary, double avgCalorieRate, int achievementDays,
                     Double weightTrend) {
        this.content = content;
        this.nutritionSummary = nutritionSummary;
        this.exerciseSummary = exerciseSummary;
        this.goalSummary = goalSummary;
        this.avgCalorieRate = avgCalorieRate;
        this.achievementDays = achievementDays;
        this.weightTrend = weightTrend;
    }
}
```

- [ ] **Step 2: WeeklyReportRepository.java에 쿼리 추가**

```java
package com.ssafy.manager.program.infrastructure.persistence;

import com.ssafy.manager.program.domain.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    boolean existsByProgramIdAndWeekNumber(Long programId, int weekNumber);
    Optional<WeeklyReport> findByProgramIdAndWeekNumber(Long programId, int weekNumber);
}
```

- [ ] **Step 3: fill() 동작 단위 테스트 작성**

파일 생성: `backend/src/test/java/com/ssafy/manager/program/domain/WeeklyReportTest.java`

```java
package com.ssafy.manager.program.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReportTest {

    @Test
    void fill_호출_후_모든_필드가_채워진다() {
        WeeklyReport report = new WeeklyReport(1L, 1);

        report.fill("ai코멘트", "영양요약", "운동요약", "목표요약", 87.5, 5, -0.3);

        assertThat(report.getContent()).isEqualTo("ai코멘트");
        assertThat(report.getNutritionSummary()).isEqualTo("영양요약");
        assertThat(report.getExerciseSummary()).isEqualTo("운동요약");
        assertThat(report.getGoalSummary()).isEqualTo("목표요약");
        assertThat(report.getAvgCalorieRate()).isEqualTo(87.5);
        assertThat(report.getAchievementDays()).isEqualTo(5);
        assertThat(report.getWeightTrend()).isEqualTo(-0.3);
    }

    @Test
    void fill_전_content는_null이다() {
        WeeklyReport report = new WeeklyReport(1L, 1);
        assertThat(report.getContent()).isNull();
    }

    @Test
    void weightTrend는_null_허용된다() {
        WeeklyReport report = new WeeklyReport(1L, 1);
        report.fill("comment", "n", "e", "g", 80.0, 4, null);
        assertThat(report.getWeightTrend()).isNull();
    }
}
```

- [ ] **Step 4: 테스트 실행**

```
cd backend && ./gradlew test --tests "com.ssafy.manager.program.domain.WeeklyReportTest" -q
```

Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 5: 커밋**

```
git add backend/src/main/java/com/ssafy/manager/program/domain/WeeklyReport.java \
        backend/src/main/java/com/ssafy/manager/program/infrastructure/persistence/WeeklyReportRepository.java \
        backend/src/test/java/com/ssafy/manager/program/domain/WeeklyReportTest.java
git commit -m "feat(program): WeeklyReport fill() 메서드 + 6개 필드 추가"
```

---

### Task 2: 데이터 수집 쿼리 추가

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/persistence/MealItemRepository.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineSessionRepository.java`
- Modify: `backend/src/main/java/com/ssafy/manager/weight/infrastructure/persistence/WeightRepository.java`

**Interfaces:**
- Produces: `MealItemRepository.DailyNutritionSummary` — 프로젝션 인터페이스 (getDate, getCalories, getProtein, getCarbs, getFat)
- Produces: `MealItemRepository.findDailyNutritionByMemberIdAndDateBetween(Long, LocalDate, LocalDate): List<DailyNutritionSummary>`
- Produces: `RoutineSessionRepository.findByMemberIdAndSessionDateBetween(Long, LocalDate, LocalDate): List<RoutineSession>`
- Produces: `WeightRepository.findByMemberIdAndRecordedDateBetween(Long, LocalDate, LocalDate): List<Weight>`

- [ ] **Step 1: MealItemRepository에 DailyNutritionSummary + 범위 쿼리 추가**

`MealItemRepository.java` 파일 끝에 아래를 추가한다 (기존 메서드는 유지):

```java
    interface DailyNutritionSummary {
        java.time.LocalDate getDate();
        double getCalories();
        double getProtein();
        double getCarbs();
        double getFat();
    }

    @Query("""
        SELECT m.effectiveDate AS date,
               COALESCE(SUM(mi.calories), 0.0) AS calories,
               COALESCE(SUM(mi.protein), 0.0)  AS protein,
               COALESCE(SUM(mi.carbs),   0.0)  AS carbs,
               COALESCE(SUM(mi.fat),     0.0)  AS fat
        FROM MealItem mi JOIN mi.meal m
        WHERE m.memberId = :memberId
          AND m.effectiveDate BETWEEN :from AND :to
        GROUP BY m.effectiveDate
        """)
    List<DailyNutritionSummary> findDailyNutritionByMemberIdAndDateBetween(
            @Param("memberId") Long memberId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
```

- [ ] **Step 2: RoutineSessionRepository에 날짜 범위 쿼리 추가**

`RoutineSessionRepository.java`에 아래 메서드 추가:

```java
    List<RoutineSession> findByMemberIdAndSessionDateBetween(Long memberId, LocalDate from, LocalDate to);
```

- [ ] **Step 3: WeightRepository에 날짜 범위 쿼리 추가**

`WeightRepository.java`에 아래 메서드 추가:

```java
    List<Weight> findByMemberIdAndRecordedDateBetween(Long memberId, LocalDate from, LocalDate to);
```

- [ ] **Step 4: 빌드 확인**

```
cd backend && ./gradlew build -x test -q
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```
git add backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/persistence/MealItemRepository.java \
        backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineSessionRepository.java \
        backend/src/main/java/com/ssafy/manager/weight/infrastructure/persistence/WeightRepository.java
git commit -m "feat(repository): 주간 코칭 데이터 수집용 날짜 범위 쿼리 추가"
```

---

### Task 3: AiCoachingClient 구현 + RestClientConfig

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/program/infrastructure/client/AiCoachingClientRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/program/infrastructure/client/AiCoachingClientResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/program/infrastructure/client/AiCoachingClient.java`
- Modify: `backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java`

**Interfaces:**
- Produces: `AiCoachingClient.weekly(AiCoachingClientRequest): AiCoachingClientResponse`
- Consumes: `aiCoachingRestClient` RestClient bean (baseUrl=`${ai.fastapi.url}`, readTimeout=60s)

- [ ] **Step 1: AiCoachingClientRequest.java 생성**

```java
package com.ssafy.manager.program.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiCoachingClientRequest(
        @JsonProperty("week_number") int weekNumber,
        @JsonProperty("health_goal") String healthGoal,
        @JsonProperty("daily_nutrition") List<DailyNutritionRecord> dailyNutrition,
        @JsonProperty("target_kcal") double targetKcal,
        @JsonProperty("target_protein_g") double targetProteinG,
        @JsonProperty("target_carb_g") double targetCarbG,
        @JsonProperty("target_fat_g") double targetFatG,
        @JsonProperty("routine_sessions") List<RoutineSessionRecord> routineSessions,
        @JsonProperty("weight_records") List<WeightRecord> weightRecords
) {
    public record DailyNutritionRecord(
            String date,
            double kcal,
            @JsonProperty("protein_g") double proteinG,
            @JsonProperty("carb_g") double carbG,
            @JsonProperty("fat_g") double fatG,
            @JsonProperty("calories_burned") double caloriesBurned
    ) {}

    public record RoutineSessionRecord(
            @JsonProperty("exercise_name") String exerciseName,
            @JsonProperty("successful_sets") int successfulSets,
            @JsonProperty("total_sets") int totalSets,
            @JsonProperty("weight_kg") double weightKg,
            @JsonProperty("session_date") String sessionDate
    ) {}

    public record WeightRecord(
            String date,
            @JsonProperty("weight_kg") double weightKg
    ) {}
}
```

- [ ] **Step 2: AiCoachingClientResponse.java 생성**

```java
package com.ssafy.manager.program.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiCoachingClientResponse(
        @JsonProperty("ai_comment") String aiComment,
        @JsonProperty("nutrition_summary") String nutritionSummary,
        @JsonProperty("exercise_summary") String exerciseSummary,
        @JsonProperty("goal_summary") String goalSummary,
        @JsonProperty("avg_calorie_rate") double avgCalorieRate,
        @JsonProperty("achievement_days") int achievementDays,
        @JsonProperty("weight_trend") Double weightTrend
) {}
```

- [ ] **Step 3: AiCoachingClient.java 생성**

```java
package com.ssafy.manager.program.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiCoachingClient {

    private final RestClient aiCoachingRestClient;

    public AiCoachingClientResponse weekly(AiCoachingClientRequest request) {
        return aiCoachingRestClient.post()
                .uri("/ai/coaching/weekly")
                .body(request)
                .retrieve()
                .body(AiCoachingClientResponse.class);
    }
}
```

- [ ] **Step 4: RestClientConfig.java에 aiCoachingRestClient bean 추가**

`RestClientConfig.java`의 기존 bean 정의들 아래에 추가:

```java
    @Bean
    RestClient aiCoachingRestClient(
            @Value("${ai.fastapi.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(60_000);  // Multi-Agent 체인 대기
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
```

- [ ] **Step 5: 빌드 확인**

```
cd backend && ./gradlew build -x test -q
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```
git add backend/src/main/java/com/ssafy/manager/program/infrastructure/client/ \
        backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java
git commit -m "feat(program): AiCoachingClient + DTO + RestClient bean 추가"
```

---

### Task 4: WeeklyCoachingDataService 구현

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/program/application/WeeklyCoachingDataService.java`
- Create: `backend/src/test/java/com/ssafy/manager/program/application/WeeklyCoachingDataServiceTest.java`

**Interfaces:**
- Consumes: `MealItemRepository.findDailyNutritionByMemberIdAndDateBetween()`
- Consumes: `RoutineSessionRepository.findByMemberIdAndSessionDateBetween()`, `SessionSetRepository.findBySessionIdIn()`
- Consumes: `WeightRepository.findByMemberIdAndRecordedDateBetween()`
- Consumes: `RoutineSessionRepository.sumCaloriesBurnedByMemberIdAndDate()`
- Produces: `WeeklyCoachingDataService.buildRequest(Program, int weekNumber): AiCoachingClientRequest`

- [ ] **Step 1: WeeklyCoachingDataService.java 생성**

```java
package com.ssafy.manager.program.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklyCoachingDataService {

    private final MealItemRepository mealItemRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final SessionSetRepository sessionSetRepository;
    private final WeightRepository weightRepository;

    @Transactional(readOnly = true)
    public AiCoachingClientRequest buildRequest(Program program, int weekNumber) {
        Long memberId = program.getMemberId();
        LocalDate weekStart = program.getStartDate().plusDays((long) (weekNumber - 1) * 7);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<AiCoachingClientRequest.DailyNutritionRecord> dailyNutrition =
                buildDailyNutrition(memberId, weekStart, weekEnd);

        List<AiCoachingClientRequest.RoutineSessionRecord> routineSessions =
                buildRoutineSessions(memberId, weekStart, weekEnd);

        List<AiCoachingClientRequest.WeightRecord> weightRecords =
                weightRepository.findByMemberIdAndRecordedDateBetween(memberId, weekStart, weekEnd)
                        .stream()
                        .map(w -> new AiCoachingClientRequest.WeightRecord(
                                w.getRecordedDate().toString(), w.getWeightKg()))
                        .toList();

        return new AiCoachingClientRequest(
                weekNumber,
                program.getType().name(),
                dailyNutrition,
                program.getTargetCalories(),
                program.getTargetProteinG(),
                program.getTargetCarbG(),
                program.getTargetFatG(),
                routineSessions,
                weightRecords
        );
    }

    private List<AiCoachingClientRequest.DailyNutritionRecord> buildDailyNutrition(
            Long memberId, LocalDate weekStart, LocalDate weekEnd) {

        Map<LocalDate, MealItemRepository.DailyNutritionSummary> nutritionByDate =
                mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(memberId, weekStart, weekEnd)
                        .stream()
                        .collect(Collectors.toMap(MealItemRepository.DailyNutritionSummary::getDate,
                                s -> s));

        return weekStart.datesUntil(weekEnd.plusDays(1))
                .map(date -> {
                    var n = nutritionByDate.get(date);
                    double burned = routineSessionRepository
                            .sumCaloriesBurnedByMemberIdAndDate(memberId, date);
                    return new AiCoachingClientRequest.DailyNutritionRecord(
                            date.toString(),
                            n != null ? n.getCalories() : 0.0,
                            n != null ? n.getProtein()  : 0.0,
                            n != null ? n.getCarbs()    : 0.0,
                            n != null ? n.getFat()      : 0.0,
                            burned
                    );
                })
                .toList();
    }

    private List<AiCoachingClientRequest.RoutineSessionRecord> buildRoutineSessions(
            Long memberId, LocalDate weekStart, LocalDate weekEnd) {

        List<RoutineSession> sessions = routineSessionRepository
                .findByMemberIdAndSessionDateBetween(memberId, weekStart, weekEnd);
        if (sessions.isEmpty()) return List.of();

        List<Long> sessionIds = sessions.stream().map(RoutineSession::getId).toList();
        List<SessionSet> sets = sessionSetRepository.findBySessionIdIn(sessionIds);

        Map<Long, LocalDate> sessionDates = sessions.stream()
                .collect(Collectors.toMap(RoutineSession::getId, RoutineSession::getSessionDate));

        return sets.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSessionId() + ":" + s.getExerciseName()))
                .values().stream()
                .map(group -> {
                    SessionSet first = group.get(0);
                    int total = group.size();
                    int successful = (int) group.stream().filter(SessionSet::isCompleted).count();
                    double weightKg = group.stream()
                            .mapToDouble(SessionSet::getActualWeightKg).average().orElse(0.0);
                    String sessionDate = sessionDates
                            .getOrDefault(first.getSessionId(), weekStart).toString();
                    return new AiCoachingClientRequest.RoutineSessionRecord(
                            first.getExerciseName(), successful, total, weightKg, sessionDate);
                })
                .toList();
    }
}
```

- [ ] **Step 2: WeeklyCoachingDataServiceTest.java 작성**

```java
package com.ssafy.manager.program.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import com.ssafy.manager.weight.domain.Weight;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WeeklyCoachingDataServiceTest {

    @Mock MealItemRepository mealItemRepository;
    @Mock RoutineSessionRepository routineSessionRepository;
    @Mock SessionSetRepository sessionSetRepository;
    @Mock WeightRepository weightRepository;

    @InjectMocks WeeklyCoachingDataService service;

    private static final LocalDate START = LocalDate.of(2026, 6, 9);

    @Test
    void week1_날짜_범위가_startDate부터_6일간이다() {
        Program program = Program.create(1L, ProgramType.MUSCLE, START, START.plusDays(27),
                2400, 120.0, 250.0, 60.0, null);

        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(
                eq(1L), eq(START), eq(START.plusDays(6)))).willReturn(List.of());
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(eq(1L), any())).willReturn(0L);
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(
                eq(1L), eq(START), eq(START.plusDays(6)))).willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(
                eq(1L), eq(START), eq(START.plusDays(6)))).willReturn(List.of());

        AiCoachingClientRequest request = service.buildRequest(program, 1);

        assertThat(request.weekNumber()).isEqualTo(1);
        assertThat(request.healthGoal()).isEqualTo("MUSCLE");
        assertThat(request.dailyNutrition()).hasSize(7);
        assertThat(request.dailyNutrition().get(0).date()).isEqualTo(START.toString());
        assertThat(request.dailyNutrition().get(6).date()).isEqualTo(START.plusDays(6).toString());
    }

    @Test
    void 체중_기록이_없으면_weightRecords가_빈_리스트다() {
        Program program = Program.create(1L, ProgramType.DIET, START, START.plusDays(27),
                1800, 90.0, 200.0, 50.0, null);

        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(any(), any())).willReturn(0L);
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(any(), any(), any()))
                .willReturn(List.of());

        AiCoachingClientRequest request = service.buildRequest(program, 1);

        assertThat(request.weightRecords()).isEmpty();
    }

    @Test
    void 체중_기록이_있으면_weightRecords에_포함된다() {
        Program program = Program.create(1L, ProgramType.DIET, START, START.plusDays(27),
                1800, 90.0, 200.0, 50.0, null);
        Weight w = Weight.create(1L, 72.5, START.plusDays(3));

        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(any(), any())).willReturn(0L);
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(any(), any(), any()))
                .willReturn(List.of(w));

        AiCoachingClientRequest request = service.buildRequest(program, 1);

        assertThat(request.weightRecords()).hasSize(1);
        assertThat(request.weightRecords().get(0).weightKg()).isEqualTo(72.5);
    }

    @Test
    void 운동_세션이_없으면_routineSessions가_빈_리스트다() {
        Program program = Program.create(1L, ProgramType.HEALTH, START, START.plusDays(27),
                2200, 100.0, 230.0, 55.0, null);

        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(any(), any())).willReturn(0L);
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(any(), any(), any()))
                .willReturn(List.of());

        AiCoachingClientRequest request = service.buildRequest(program, 1);

        assertThat(request.routineSessions()).isEmpty();
    }
}
```

- [ ] **Step 3: 테스트 실행**

```
cd backend && ./gradlew test --tests "com.ssafy.manager.program.application.WeeklyCoachingDataServiceTest" -q
```

Expected: BUILD SUCCESSFUL, 4 tests passed

- [ ] **Step 4: 커밋**

```
git add backend/src/main/java/com/ssafy/manager/program/application/WeeklyCoachingDataService.java \
        backend/src/test/java/com/ssafy/manager/program/application/WeeklyCoachingDataServiceTest.java
git commit -m "feat(program): WeeklyCoachingDataService — 7일 데이터 수집 후 AiCoachingClientRequest 조립"
```

---

### Task 5: WeeklyReportService 배치 연동

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/program/application/WeeklyReportService.java`
- Modify: `backend/src/test/java/com/ssafy/manager/program/application/WeeklyReportServiceTest.java`

**Interfaces:**
- Consumes: `WeeklyCoachingDataService.buildRequest(Program, int): AiCoachingClientRequest`
- Consumes: `AiCoachingClient.weekly(AiCoachingClientRequest): AiCoachingClientResponse`
- Consumes: `WeeklyReport.fill(...)`

- [ ] **Step 1: WeeklyReportService.java 수정**

```java
package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClient;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientResponse;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final ProgramRepository programRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final WeeklyCoachingDataService weeklyCoachingDataService;
    private final AiCoachingClient aiCoachingClient;

    @Transactional
    public void createStubs(LocalDate today) {
        programRepository.findAllByStatus(ProgramStatus.ACTIVE).forEach(p -> {
            long daysElapsed = ChronoUnit.DAYS.between(p.getStartDate(), today);
            if (daysElapsed < 7) return;

            int weekNumber = (int) (daysElapsed / 7);
            if (!weeklyReportRepository.existsByProgramIdAndWeekNumber(p.getId(), weekNumber)) {
                WeeklyReport stub = weeklyReportRepository.save(new WeeklyReport(p.getId(), weekNumber));
                fillWithAiContent(stub, p, weekNumber);
            }
        });
    }

    private void fillWithAiContent(WeeklyReport stub, Program program, int weekNumber) {
        try {
            AiCoachingClientRequest request = weeklyCoachingDataService.buildRequest(program, weekNumber);
            AiCoachingClientResponse response = aiCoachingClient.weekly(request);
            stub.fill(
                    response.aiComment(),
                    response.nutritionSummary(),
                    response.exerciseSummary(),
                    response.goalSummary(),
                    response.avgCalorieRate(),
                    response.achievementDays(),
                    response.weightTrend()
            );
            weeklyReportRepository.save(stub);
        } catch (Exception e) {
            log.warn("주간 코칭 AI 호출 실패 — programId={}, week={}: {}",
                    program.getId(), weekNumber, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: WeeklyReportServiceTest.java 수정**

기존 테스트에 `@Mock` 두 개 추가 후 기존 테스트 케이스 `Program_시작_7일_경과_시_WeeklyReport_stub이_생성된다`를 업데이트하고 새 케이스 2개 추가:

```java
package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClient;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientResponse;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock ProgramRepository programRepository;
    @Mock WeeklyReportRepository weeklyReportRepository;
    @Mock WeeklyCoachingDataService weeklyCoachingDataService;
    @Mock AiCoachingClient aiCoachingClient;

    @InjectMocks WeeklyReportService weeklyReportService;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);

    @Test
    void Program_시작_7일_경과_시_stub이_생성되고_AI가_호출된다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(false);

        WeeklyReport stub = new WeeklyReport(null, 1);
        given(weeklyReportRepository.save(any(WeeklyReport.class))).willReturn(stub);

        AiCoachingClientRequest mockRequest = mock(AiCoachingClientRequest.class);
        given(weeklyCoachingDataService.buildRequest(any(), eq(1))).willReturn(mockRequest);

        AiCoachingClientResponse mockResponse = new AiCoachingClientResponse(
                "코멘트", "영양요약", "운동요약", "목표요약", 85.0, 5, -0.2);
        given(aiCoachingClient.weekly(mockRequest)).willReturn(mockResponse);

        weeklyReportService.createStubs(TODAY);

        verify(weeklyCoachingDataService).buildRequest(any(), eq(1));
        verify(aiCoachingClient).weekly(mockRequest);
        verify(weeklyReportRepository, times(2)).save(any(WeeklyReport.class));
    }

    @Test
    void AI_호출_실패_시_stub만_저장되고_예외는_전파되지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(false);

        WeeklyReport stub = new WeeklyReport(null, 1);
        given(weeklyReportRepository.save(any(WeeklyReport.class))).willReturn(stub);
        given(weeklyCoachingDataService.buildRequest(any(), anyInt()))
                .willThrow(new RuntimeException("FastAPI 연결 실패"));

        weeklyReportService.createStubs(TODAY);  // 예외 전파 없이 정상 종료

        // stub은 1번 저장, AI 실패 후 두 번째 save 없음
        verify(weeklyReportRepository, times(1)).save(any(WeeklyReport.class));
        assertThat(stub.getContent()).isNull();
    }

    @Test
    void WeeklyReport가_이미_있으면_중복_생성하지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(true);

        weeklyReportService.createStubs(TODAY);

        verify(weeklyReportRepository, never()).save(any(WeeklyReport.class));
        verifyNoInteractions(weeklyCoachingDataService, aiCoachingClient);
    }

    @Test
    void 경과일이_7일_미만이면_WeeklyReport를_생성하지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(6), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));

        weeklyReportService.createStubs(TODAY);

        verify(weeklyReportRepository, never()).save(any(WeeklyReport.class));
        verifyNoInteractions(weeklyCoachingDataService, aiCoachingClient);
    }
}
```

- [ ] **Step 3: 테스트 실행**

```
cd backend && ./gradlew test --tests "com.ssafy.manager.program.application.WeeklyReportServiceTest" -q
```

Expected: BUILD SUCCESSFUL, 4 tests passed

- [ ] **Step 4: 전체 테스트 확인**

```
cd backend && ./gradlew test -q
```

Expected: BUILD SUCCESSFUL (기존 테스트 포함 전체 통과)

- [ ] **Step 5: 커밋**

```
git add backend/src/main/java/com/ssafy/manager/program/application/WeeklyReportService.java \
        backend/src/test/java/com/ssafy/manager/program/application/WeeklyReportServiceTest.java
git commit -m "feat(program): WeeklyReportService — 배치 stub 생성 후 AI 코칭 호출 연동"
```

---

### Task 6: GET /programs/{id}/weekly-reports/{week}

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/program/presentation/dto/WeeklyReportResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/program/presentation/WeeklyReportController.java`
- Create: `backend/src/test/java/com/ssafy/manager/program/presentation/WeeklyReportControllerTest.java`

**Interfaces:**
- Consumes: `WeeklyReportRepository.findByProgramIdAndWeekNumber(Long, int): Optional<WeeklyReport>`
- Consumes: `ProgramRepository.findById(Long): Optional<Program>` (소유권 검증)
- Produces: `GET /programs/{programId}/weekly-reports/{weekNumber}` → `WeeklyReportResponse`

- [ ] **Step 1: WeeklyReportResponse.java 생성**

```java
package com.ssafy.manager.program.presentation.dto;

import com.ssafy.manager.program.domain.WeeklyReport;

public record WeeklyReportResponse(
        Long id,
        int weekNumber,
        String content,
        String nutritionSummary,
        String exerciseSummary,
        String goalSummary,
        double avgCalorieRate,
        int achievementDays,
        Double weightTrend
) {
    public static WeeklyReportResponse from(WeeklyReport report) {
        return new WeeklyReportResponse(
                report.getId(),
                report.getWeekNumber(),
                report.getContent(),
                report.getNutritionSummary(),
                report.getExerciseSummary(),
                report.getGoalSummary(),
                report.getAvgCalorieRate(),
                report.getAchievementDays(),
                report.getWeightTrend()
        );
    }
}
```

- [ ] **Step 2: WeeklyReportController.java 생성**

```java
package com.ssafy.manager.program.presentation;

import com.ssafy.manager.global.exception.ForbiddenException;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import com.ssafy.manager.program.presentation.dto.WeeklyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final ProgramRepository programRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    @GetMapping("/{programId}/weekly-reports/{weekNumber}")
    public ResponseEntity<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long programId,
            @PathVariable int weekNumber
    ) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NoSuchElementException("Program을 찾을 수 없습니다."));
        if (!program.getMemberId().equals(memberId)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }
        WeeklyReport report = weeklyReportRepository.findByProgramIdAndWeekNumber(programId, weekNumber)
                .orElseThrow(() -> new NoSuchElementException("WeeklyReport를 찾을 수 없습니다."));
        return ResponseEntity.ok(WeeklyReportResponse.from(report));
    }
}
```

- [ ] **Step 3: WeeklyReportControllerTest.java 작성**

```java
package com.ssafy.manager.program.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeeklyReportController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class WeeklyReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProgramRepository programRepository;
    @MockitoBean WeeklyReportRepository weeklyReportRepository;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 주간_리포트를_정상_조회한다() throws Exception {
        Long programId = 10L;
        Program program = Program.create(MEMBER_ID, ProgramType.MUSCLE, LocalDate.now(), LocalDate.now().plusDays(27),
                2400, 120.0, 250.0, 60.0, null);
        given(programRepository.findById(programId)).willReturn(Optional.of(program));

        WeeklyReport report = new WeeklyReport(programId, 1);
        report.fill("코멘트", "영양요약", "운동요약", "목표요약", 87.5, 5, -0.2);
        given(weeklyReportRepository.findByProgramIdAndWeekNumber(programId, 1))
                .willReturn(Optional.of(report));

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", programId)
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekNumber").value(1))
                .andExpect(jsonPath("$.content").value("코멘트"))
                .andExpect(jsonPath("$.nutritionSummary").value("영양요약"))
                .andExpect(jsonPath("$.avgCalorieRate").value(87.5))
                .andExpect(jsonPath("$.achievementDays").value(5))
                .andExpect(jsonPath("$.weightTrend").value(-0.2));
    }

    @Test
    void 다른_회원_program_접근_시_403을_반환한다() throws Exception {
        Long programId = 10L;
        Program program = Program.create(99L, ProgramType.DIET, LocalDate.now(), LocalDate.now().plusDays(27),
                1800, 90.0, 200.0, 50.0, null);
        given(programRepository.findById(programId)).willReturn(Optional.of(program));

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", programId)
                        .with(authentication(AUTH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void WeeklyReport가_없으면_404를_반환한다() throws Exception {
        Long programId = 10L;
        Program program = Program.create(MEMBER_ID, ProgramType.HEALTH, LocalDate.now(), LocalDate.now().plusDays(27),
                2200, 100.0, 230.0, 55.0, null);
        given(programRepository.findById(programId)).willReturn(Optional.of(program));
        given(weeklyReportRepository.findByProgramIdAndWeekNumber(programId, 1))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", programId)
                        .with(authentication(AUTH)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 인증_없이_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/programs/10/weekly-reports/1"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 4: 테스트 실행**

```
cd backend && ./gradlew test --tests "com.ssafy.manager.program.presentation.WeeklyReportControllerTest" -q
```

Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 5: 전체 테스트 확인**

```
cd backend && ./gradlew test -q
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```
git add backend/src/main/java/com/ssafy/manager/program/presentation/WeeklyReportController.java \
        backend/src/main/java/com/ssafy/manager/program/presentation/dto/WeeklyReportResponse.java \
        backend/src/test/java/com/ssafy/manager/program/presentation/WeeklyReportControllerTest.java
git commit -m "feat(program): GET /programs/{id}/weekly-reports/{week} 구현"
```
