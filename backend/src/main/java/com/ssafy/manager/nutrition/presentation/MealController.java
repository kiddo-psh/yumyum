package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.MealService;
import com.ssafy.manager.nutrition.presentation.dto.MealRequest;
import com.ssafy.manager.nutrition.presentation.dto.MealResponse;
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

    @PostMapping
    public ResponseEntity<Void> record(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MealRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Long mealId = mealService.record(request.toCommand(memberId), LocalDateTime.now());
        return ResponseEntity.created(
                uriBuilder.path("/meals/{id}").buildAndExpand(mealId).toUri()
        ).build();
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
