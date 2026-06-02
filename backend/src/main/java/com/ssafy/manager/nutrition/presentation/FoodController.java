package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.infrastructure.persistence.FoodRepository;
import com.ssafy.manager.nutrition.presentation.dto.FoodResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodRepository foodRepository;

    @GetMapping
    public List<FoodResponse> search(@RequestParam(defaultValue = "") String query) {
        return foodRepository.findByNameContaining(query).stream()
                .map(FoodResponse::from)
                .toList();
    }
}
