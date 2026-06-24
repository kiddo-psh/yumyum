package com.ssafy.manager.nutrition.infrastructure.client;

import com.ssafy.manager.nutrition.infrastructure.persistence.FoodJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FoodDataInitializer implements ApplicationRunner {

    private final FoodJpaRepository foodJpaRepository;
    private final FoodBulkLoadService foodBulkLoadService;

    @Override
    public void run(ApplicationArguments args) {
        long count = foodJpaRepository.count();
        if (count == 0) {
            log.info("[FoodInit] foods 테이블 비어있음 → 백그라운드 전체 로딩 시작");
            foodBulkLoadService.loadAll();
        } else {
            log.info("[FoodInit] foods 테이블에 {}건 존재, 초기 로딩 스킵", count);
        }
    }
}
