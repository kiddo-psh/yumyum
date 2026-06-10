package com.ssafy.manager.weight.presentation;

import com.ssafy.manager.weight.application.WeightService;
import com.ssafy.manager.weight.domain.Weight;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/weights")
@RequiredArgsConstructor
public class WeightController {

    private final WeightService weightService;

    @PostMapping
    public ResponseEntity<WeightResponse> record(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CreateWeightRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Weight saved = weightService.record(memberId, request.weightKg(), request.recordedDate());
        return ResponseEntity.created(
                uriBuilder.path("/weights/{id}").buildAndExpand(saved.getId()).toUri()
        ).body(WeightResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<WeightResponse>> list(@AuthenticationPrincipal Long memberId) {
        List<WeightResponse> result = weightService.findByMember(memberId).stream()
                .map(WeightResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id
    ) {
        weightService.delete(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
