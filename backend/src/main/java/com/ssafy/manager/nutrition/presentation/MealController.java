package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.growth.application.EarnedBadgeCollector;
import com.ssafy.manager.nutrition.application.MealItemCommand;
import com.ssafy.manager.nutrition.application.MealService;
import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.presentation.dto.MealItemRequest;
import com.ssafy.manager.nutrition.presentation.dto.MealRequest;
import com.ssafy.manager.nutrition.presentation.dto.MealResponse;
import com.ssafy.manager.nutrition.presentation.dto.PhotoMealRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final EarnedBadgeCollector earnedBadgeCollector;

    @PostMapping("/photo")
    public ResponseEntity<MealResponse> recordFromPhoto(
            @AuthenticationPrincipal Long memberId,
            @RequestBody PhotoMealRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        LocalDateTime now = LocalDateTime.now();
        Meal meal = mealService.recordFromPhoto(request.toCommand(memberId), now);
        return ResponseEntity.created(
                uriBuilder.path("/meals/{id}").buildAndExpand(meal.getId()).toUri()
        ).body(MealResponse.from(meal, earnedBadgeCollector.getEarned()));
    }

    @PostMapping
    public ResponseEntity<MealResponse> record(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MealRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        LocalDateTime now = LocalDateTime.now();
        Meal meal = mealService.record(request.toCommand(memberId, request.type()), now);
        return ResponseEntity.created(
                uriBuilder.path("/meals/{id}").buildAndExpand(meal.getId()).toUri()
        ).body(MealResponse.from(meal, earnedBadgeCollector.getEarned()));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<MealResponse> addItem(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @RequestBody MealItemRequest request
    ) {
        return ResponseEntity.ok(
                MealResponse.from(mealService.addItem(id, memberId,
                        new MealItemCommand(request.foodCode(), request.amountGrams())))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id
    ) {
        mealService.delete(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<MealResponse>> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<MealResponse> meals = mealService.listByDate(memberId, targetDate).stream()
                .map(MealResponse::from)
                .toList();
        return ResponseEntity.ok(meals);
    }
}
