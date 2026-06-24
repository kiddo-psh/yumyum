package com.ssafy.manager.weight.application;

import com.ssafy.manager.weight.domain.Weight;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.manager.global.exception.ForbiddenException;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class WeightService {

    private final WeightRepository weightRepository;

    @Transactional
    public Weight record(Long memberId, double weightKg, LocalDate recordedDate) {
        return weightRepository.findByMemberIdAndRecordedDate(memberId, recordedDate)
                .map(existing -> {
                    existing.update(weightKg);
                    return existing;
                })
                .orElseGet(() -> weightRepository.save(Weight.create(memberId, weightKg, recordedDate)));
    }

    @Transactional(readOnly = true)
    public List<Weight> findByMember(Long memberId) {
        return weightRepository.findByMemberIdOrderByRecordedDateDesc(memberId);
    }

    @Transactional
    public void delete(Long memberId, Long weightId) {
        Weight weight = weightRepository.findById(weightId)
                .orElseThrow(() -> new NoSuchElementException("체중 기록을 찾을 수 없습니다."));
        if (!weight.getMemberId().equals(memberId)) {
            throw new ForbiddenException("본인의 체중 기록만 삭제할 수 있습니다.");
        }
        weightRepository.delete(weight);
    }
}
