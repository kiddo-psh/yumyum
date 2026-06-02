package com.ssafy.manager.nutrition.infrastructure.persistence;

import com.ssafy.manager.nutrition.domain.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByNameContaining(String keyword);
}
