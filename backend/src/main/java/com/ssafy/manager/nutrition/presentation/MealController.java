package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.MealService;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nutrition.presentation.dto.MealRequest;
import com.ssafy.manager.nutrition.presentation.dto.MealResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final MealRepository mealRepository;

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
            @RequestParam LocalDate date
    ) {
        return mealRepository.findAllByMemberIdAndDate(memberId, date).stream()
                .map(MealResponse::from)
                .toList();
    }
}
