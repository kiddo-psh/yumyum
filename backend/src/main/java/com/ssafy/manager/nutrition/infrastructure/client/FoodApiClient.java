package com.ssafy.manager.nutrition.infrastructure.client;

import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FoodApiClient implements FoodRepository {

    @Override
    public List<Food> search(String keyword) {
        // TODO: 공공데이터 식품영양성분 OpenAPI 호출
        return List.of();
    }

    @Override
    public Optional<Food> findByCode(String foodCode) {
        // TODO: 공공데이터 식품영양성분 OpenAPI 호출
        return Optional.empty();
    }
}
