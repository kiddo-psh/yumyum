package com.ssafy.manager.nutrition.infrastructure.client;

import com.ssafy.manager.global.config.FoodApiProperties;
import com.ssafy.manager.global.exception.FoodApiException;
import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import com.ssafy.manager.nutrition.infrastructure.client.dto.FoodApiResponse;
import com.ssafy.manager.nutrition.infrastructure.persistence.FoodEntity;
import com.ssafy.manager.nutrition.infrastructure.persistence.FoodJpaRepository;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class FoodApiClient implements FoodRepository {

    private final RestClient foodApiRestClient;
    private final FoodApiProperties properties;
    private final FoodJpaRepository foodJpaRepository;

    @Override
    @Transactional
    public List<Food> search(String keyword) {
        // DB에 이미 동일 키워드 결과가 있으면 외부 API를 호출하지 않는다.
        List<FoodEntity> cached = foodJpaRepository.findByNameContaining(keyword);
        if (!cached.isEmpty()) {
            return cached.stream().map(FoodEntity::toDomain).toList();
        }

        List<Food> results = fetchFromApi(keyword);
        if (!results.isEmpty()) {
            List<FoodEntity> entities = results.stream().map(FoodEntity::from).toList();
            foodJpaRepository.saveAll(entities);
        }
        return results;
    }

    @Override
    public Optional<Food> findByCode(String foodCode) {
        return foodJpaRepository.findById(foodCode).map(FoodEntity::toDomain);
    }

    private List<Food> fetchFromApi(String keyword) {
        try {
            FoodApiResponse response = foodApiRestClient.get()
                    .uri(uriBuilder -> buildUri(uriBuilder, Map.of("FOOD_NM_KR", keyword)))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, ((req, res) -> {
                        throw new FoodApiException("인증키 또는 요청 파라미터 오류: " + res.getStatusCode());
                    }))
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new FoodApiException("공공 데이터 포털에서 오류 발생: " + res.getStatusCode());
                    })
                    .body(FoodApiResponse.class);
            return toFoods(response);
        } catch (FoodApiException e) {
            log.warn("Food API 상태 코드 오류: keyword={}, msg={}", keyword, e.getMessage());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Food API 연결 실패(타임아웃/네트워크): keyword={}", keyword, e);
            return List.of();
        } catch (UnknownContentTypeException e) {
            log.warn("Food API 응답 형식 오류(역직렬화 실패): keyword={}", keyword, e);
            return List.of();
        } catch (RestClientException e) {
            log.warn("Food API 호출 실패(기타): keyword={}", keyword, e);
            return List.of();
        }
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
