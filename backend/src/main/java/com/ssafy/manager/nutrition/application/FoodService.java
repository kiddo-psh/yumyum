package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;

    public List<Food> search(String keyword) {
        return foodRepository.search(keyword);
    }
}
