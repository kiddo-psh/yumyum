package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.MealService;
import com.ssafy.manager.nutrition.presentation.dto.MealRequest;
import com.ssafy.manager.nutrition.presentation.dto.MealResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void record(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestBody MealRequest request
    ) {
        mealService.record(request.toCommand(memberId), LocalDateTime.now());
    }

    @GetMapping
    public List<MealResponse> list(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return mealService.listByDate(memberId, targetDate).stream()
                .map(MealResponse::from)
                .toList();
    }
}
