package com.ssafy.manager.nutrition.domain;

import java.util.List;
import java.util.Optional;

public interface FoodRepository {
    List<Food> search(String keyword);
    Optional<Food> findByCode(String foodCode);
}
