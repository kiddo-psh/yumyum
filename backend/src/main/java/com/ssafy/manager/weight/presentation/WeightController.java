package com.ssafy.manager.weight.presentation;

import com.ssafy.manager.weight.application.WeightService;
import com.ssafy.manager.weight.domain.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/weights")
@RequiredArgsConstructor
public class WeightController {

    private final WeightService weightService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WeightResponse record(@RequestBody CreateWeightRequest request) {
        Weight saved = weightService.record(request.memberId(), request.weightKg(), request.recordedDate());
        return WeightResponse.from(saved);
    }

    @GetMapping
    public List<WeightResponse> list(@RequestParam Long memberId) {
        return weightService.findByMember(memberId).stream()
                .map(WeightResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam Long memberId) {
        weightService.delete(memberId, id);
    }
}
