package com.ssafy.manager.nutrition.infrastructure.persistence;

import com.ssafy.manager.nutrition.domain.MealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MealItemRepository extends JpaRepository<MealItem, Long> {

    @Query("SELECT COALESCE(SUM(mi.calories), 0.0) FROM MealItem mi JOIN mi.meal m WHERE m.memberId = :memberId AND m.effectiveDate = :effectiveDate")
    double sumCaloriesByMemberIdAndEffectiveDate(@Param("memberId") Long memberId, @Param("effectiveDate") LocalDate effectiveDate);

    @Query("SELECT COALESCE(SUM(mi.protein), 0.0) FROM MealItem mi JOIN mi.meal m WHERE m.memberId = :memberId AND m.effectiveDate = :effectiveDate")
    double sumProteinByMemberIdAndEffectiveDate(@Param("memberId") Long memberId, @Param("effectiveDate") LocalDate effectiveDate);

    @Query("SELECT COALESCE(SUM(mi.carbs), 0.0) FROM MealItem mi JOIN mi.meal m WHERE m.memberId = :memberId AND m.effectiveDate = :effectiveDate")
    double sumCarbsByMemberIdAndEffectiveDate(@Param("memberId") Long memberId, @Param("effectiveDate") LocalDate effectiveDate);

    @Query("SELECT COALESCE(SUM(mi.fat), 0.0) FROM MealItem mi JOIN mi.meal m WHERE m.memberId = :memberId AND m.effectiveDate = :effectiveDate")
    double sumFatByMemberIdAndEffectiveDate(@Param("memberId") Long memberId, @Param("effectiveDate") LocalDate effectiveDate);

    @Query("SELECT mi FROM MealItem mi JOIN mi.meal m WHERE m.memberId = :memberId AND m.effectiveDate = :date")
    List<MealItem> findAllByMemberIdAndEffectiveDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);
}
