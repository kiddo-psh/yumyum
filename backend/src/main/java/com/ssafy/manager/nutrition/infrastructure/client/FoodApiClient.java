package com.ssafy.manager.nutrition.infrastructure.client;

import com.ssafy.manager.global.config.FoodApiProperties;
import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import com.ssafy.manager.nutrition.infrastructure.client.dto.FoodApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FoodApiClient implements FoodRepository {

    private final RestClient foodApiRestClient;
    private final FoodApiProperties properties;

    @Override
    public List<Food> search(String keyword) {
        try {
            FoodApiResponse response = foodApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("type", properties.responseType())
                            .queryParam("numOfRows", properties.pageSize())
                            .queryParam("pageNo", 1)
                            .queryParam("FOOD_NM_KR", keyword)
                            .build())
                    .retrieve()
                    .body(FoodApiResponse.class);
            return toFoods(response);
        } catch (Exception e) {
            log.warn("Food search failed: keyword={}", keyword, e);
            return List.of();
        }
    }

    @Override
    public Optional<Food> findByCode(String foodCode) {
        try {
            FoodApiResponse response = foodApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("type", properties.responseType())
                            .queryParam("numOfRows", 1)
                            .queryParam("pageNo", 1)
                            .queryParam("FOOD_CD", foodCode)
                            .build())
                    .retrieve()
                    .body(FoodApiResponse.class);
            return toFoods(response).stream().findFirst();
        } catch (Exception e) {
            log.warn("Food lookup failed: code={}", foodCode, e);
            return Optional.empty();
        }
    }

    private List<Food> toFoods(FoodApiResponse response) {
        if (response == null || response.getBody() == null
                || response.getBody().getItems() == null
                || response.getBody().getItems().getItem() == null) {
            return List.of();
        }
        return response.getBody().getItems().getItem().stream()
                .map(this::toFood)
                .toList();
    }

    private Food toFood(FoodApiResponse.Item item) {
        return Food.of(
                item.getFoodCode(),
                item.getFoodName(),
                parseServingSize(item.getServingSize()),
                parseDouble(item.getCalories()),
                parseDouble(item.getCarbs()),
                parseDouble(item.getProtein()),
                parseDouble(item.getFat()),
                parseDouble(item.getFiber())
        );
    }

    private static int parseServingSize(String value) {
        if (value == null || value.isBlank()) return 100;
        try {
            int parsed = (int) Double.parseDouble(value.replaceAll("[^0-9.]", ""));
            return parsed > 0 ? parsed : 100;
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
