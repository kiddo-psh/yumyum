package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.FoodService;
import com.ssafy.manager.nutrition.presentation.dto.FoodResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ResponseEntity<List<FoodResponse>> search(@RequestParam(defaultValue = "") String query) {
        List<FoodResponse> foods = foodService.search(query).stream()
                .map(FoodResponse::from)
                .toList();
        return ResponseEntity.ok(foods);
    }
}
