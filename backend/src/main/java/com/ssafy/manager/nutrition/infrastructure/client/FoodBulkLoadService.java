package com.ssafy.manager.nutrition.infrastructure.client;

import com.ssafy.manager.global.config.FoodApiProperties;
import com.ssafy.manager.nutrition.infrastructure.client.dto.FoodApiResponse;
import com.ssafy.manager.nutrition.infrastructure.client.dto.FoodApiResponse.Item;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodBulkLoadService {

    private static final int BULK_PAGE_SIZE = 500;

    private final RestClient foodApiRestClient;
    private final FoodApiProperties properties;
    private final JdbcTemplate jdbcTemplate;

    @Async
    public void loadAll() {
        log.info("[FoodBulk] 식품안전처 전체 데이터 로딩 시작");
        try {
            int totalCount = fetchTotalCount();
            if (totalCount == 0) {
                log.warn("[FoodBulk] totalCount=0, 로딩 중단");
                return;
            }

            int totalPages = (int) Math.ceil((double) totalCount / BULK_PAGE_SIZE);
            log.info("[FoodBulk] 총 {}건, {}페이지 처리 예정", totalCount, totalPages);

            int saved = 0;
            for (int page = 1; page <= totalPages; page++) {
                List<Item> items = fetchPage(page);
                if (items.isEmpty()) continue;
                saved += batchInsert(items);

                if (page % 10 == 0) {
                    log.info("[FoodBulk] {}/{} 페이지 완료, 누적 {}건 저장", page, totalPages, saved);
                }
            }
            log.info("[FoodBulk] 완료 — 총 {}건 저장", saved);
        } catch (Exception e) {
            log.error("[FoodBulk] 로딩 실패", e);
        }
    }

    private int fetchTotalCount() {
        FoodApiResponse response = foodApiRestClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, 1, 1))
                .retrieve()
                .body(FoodApiResponse.class);
        if (response == null || response.getBody() == null) return 0;
        return response.getBody().getTotalCount();
    }

    private List<Item> fetchPage(int page) {
        FoodApiResponse response = foodApiRestClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, BULK_PAGE_SIZE, page))
                .retrieve()
                .body(FoodApiResponse.class);
        if (response == null || response.getBody() == null
                || response.getBody().getItems() == null) {
            return List.of();
        }
        return response.getBody().getItems();
    }

    private int batchInsert(List<Item> items) {
        String sql = """
                INSERT IGNORE INTO foods
                    (food_code, name, calories_per100g, carbs_per100g, protein_per100g, fat_per100g, fiber_per100g)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        int[][] result = jdbcTemplate.batchUpdate(sql, items, 500, (ps, item) -> {
            double servingRatio = servingRatio(item.getServingSize());
            ps.setString(1, item.getFoodCode());
            ps.setString(2, item.getFoodName());
            ps.setDouble(3, parseDouble(item.getCalories()) * servingRatio);
            ps.setDouble(4, parseDouble(item.getCarbs())    * servingRatio);
            ps.setDouble(5, parseDouble(item.getProtein())  * servingRatio);
            ps.setDouble(6, parseDouble(item.getFat())      * servingRatio);
            ps.setDouble(7, parseDouble(item.getFiber())    * servingRatio);
        });
        return items.size();
    }

    private URI buildUri(UriBuilder uriBuilder, int numOfRows, int pageNo) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("serviceKey", properties.serviceKey());
        return uriBuilder
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("type", properties.responseType())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .build(vars);
    }

    private static double servingRatio(String servingSize) {
        if (servingSize == null || servingSize.isBlank()) return 1.0;
        try {
            double grams = Double.parseDouble(servingSize.replaceAll("[^0-9.]", ""));
            return grams > 0 ? 100.0 / grams : 1.0;
        } catch (NumberFormatException e) {
            return 1.0;
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
