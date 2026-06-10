package com.ssafy.manager.weight.application;

import com.ssafy.manager.weight.domain.Weight;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class WeightService {

    private final WeightRepository weightRepository;

    @Transactional
    public Weight record(Long memberId, double weightKg, LocalDate recordedDate) {
        return weightRepository.save(Weight.create(memberId, weightKg, recordedDate));
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
            throw new NoSuchElementException("체중 기록을 찾을 수 없습니다.");
        }
        weightRepository.delete(weight);
    }
}
