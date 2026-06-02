package com.ssafy.manager.nutrition.infrastructure.persistence;

import com.ssafy.manager.nutrition.domain.MealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface MealItemRepository extends JpaRepository<MealItem, Long> {

    @Query("SELECT COALESCE(SUM(mi.calories), 0.0) FROM MealItem mi JOIN mi.meal m WHERE m.memberId = :memberId AND m.effectiveDate = :effectiveDate")
    double sumCaloriesByMemberIdAndEffectiveDate(@Param("memberId") Long memberId, @Param("effectiveDate") LocalDate effectiveDate);
}
