package com.ssafy.manager.nutrition.infrastructure.persistence;

import com.ssafy.manager.nutrition.domain.Meal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findAllByMemberIdAndDate(Long memberId, LocalDate date);
}
