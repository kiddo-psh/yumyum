package com.ssafy.manager.nutrition.infrastructure.persistence;

import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.domain.MealSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findAllByMemberIdAndDate(Long memberId, LocalDate date);

    /** 기록 경로(source)별 식사 수 (PHOTO_KING 평가용). */
    long countByMemberIdAndSource(Long memberId, MealSource source);

    /** 밤(22:00~04:00, hour ≥ 22 또는 < 4)에 기록된 식사 수 (NIGHT_EATER 평가용). */
    @Query("SELECT COUNT(m) FROM Meal m WHERE m.memberId = :memberId AND m.recordedAt IS NOT NULL AND " +
           "(FUNCTION('HOUR', m.recordedAt) >= 22 OR FUNCTION('HOUR', m.recordedAt) < 4)")
    long countNightMealsByMemberId(@Param("memberId") Long memberId);

    int countByMemberIdAndEffectiveDate(Long memberId, LocalDate effectiveDate);

    /** 한 끼의 식이섬유 합이 minFiber(g) 이상인 식사 수 (VEGGIE_MANIA 평가용). */
    @Query("SELECT COUNT(m) FROM Meal m WHERE m.memberId = :memberId AND " +
           "(SELECT COALESCE(SUM(mi.fiber), 0.0) FROM MealItem mi WHERE mi.meal = m) >= :minFiber")
    long countFiberRichMealsByMemberId(@Param("memberId") Long memberId,
                                       @Param("minFiber") double minFiber);

    /** foodName에 keyword가 포함된 항목을 가진 식사 수 (CHICKEN_BREAST_EVANGELIST 평가용). */
    @Query("SELECT COUNT(DISTINCT m) FROM Meal m JOIN m.items mi " +
           "WHERE m.memberId = :memberId AND mi.foodName LIKE CONCAT('%', :keyword, '%')")
    long countMealsContainingFoodNameByMemberId(@Param("memberId") Long memberId,
                                                @Param("keyword") String keyword);
}
