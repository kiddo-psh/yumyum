package com.ssafy.manager.nutrition.infrastructure.client;

import com.ssafy.manager.global.config.FoodApiProperties;
import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import com.ssafy.manager.nutrition.infrastructure.client.dto.FoodApiResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import org.springframework.web.util.UriBuilder;

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
                    .uri(uriBuilder -> buildUri(uriBuilder, Map.of("FOOD_NM_KR", keyword)))
                    .retrieve()
                    .body(FoodApiResponse.class);
            return toFoods(response);
        } catch (Exception e) {
            log.warn("Food search failed: keyword={}", keyword, e);
            return List.of();
        }
    }

    // TODO: 식품영양처 API엔 code를 통해 음식을 가져올 순 없음. 이 메서드는 DB에 접근하는 Repository만 사용해야할듯.
    @Override
    public Optional<Food> findByCode(String foodCode) {
        return Optional.empty();
    }

    private URI buildUri(UriBuilder uriBuilder, Map<String, String> extraParams) {
        uriBuilder.queryParam("serviceKey", "{serviceKey}")
                .queryParam("type", properties.responseType())
                .queryParam("numOfRows", properties.pageSize())
                .queryParam("pageNo", 1);

        Map<String, String> variables = new HashMap<>();
        variables.put("serviceKey", properties.serviceKey());

        extraParams.forEach((name, value) -> {
            uriBuilder.queryParam(name, "{" + name + "}");
            variables.put(name, value);
        });

        return uriBuilder.build(variables);
    }

    private List<Food> toFoods(FoodApiResponse response) {
        if (response == null || response.getBody() == null
                || response.getBody().getItems() == null) {
            return List.of();
        }
        return response.getBody().getItems().stream()
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
