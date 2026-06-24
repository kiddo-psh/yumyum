package com.ssafy.manager.program.application;

import com.ssafy.manager.member.application.MemberOnboardedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * 온보딩 완료 후 Program(+오늘 DailyGoal)을 자동 생성한다.
 *
 * AFTER_COMMIT + @Async:
 *  - 온보딩 트랜잭션이 커밋된 뒤 별도 스레드에서 실행되므로
 *    FastAPI 오류 시 온보딩 자체가 롤백되지 않는다.
 *  - 생성한 회원 데이터가 이미 DB에 반영된 상태에서 읽힌다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramOnboardingListener {

    private final ProgramService programService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void on(MemberOnboardedEvent event) {
        try {
            programService.create(event.memberId(), LocalDate.now(), 4);
            log.info("[ProgramOnboarding] Program 생성 완료: memberId={}", event.memberId());
        } catch (IllegalStateException e) {
            log.info("[ProgramOnboarding] 이미 활성 Program 있음, 건너뜀: memberId={}", event.memberId());
        } catch (Exception e) {
            log.error("[ProgramOnboarding] Program 생성 실패: memberId={}", event.memberId(), e);
        }
    }
}
