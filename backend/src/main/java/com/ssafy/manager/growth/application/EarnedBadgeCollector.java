package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

/**
 * 한 요청 동안 새로 획득한 Badge를 모으는 수집기 (docs/adr/0002).
 *
 * <p>뱃지 grant는 트랜잭션 안 이벤트 리스너에서 일어나 값을 반환하지 않는다.
 * 동기 이벤트는 같은 요청·같은 스레드에서 돌기 때문에, 리스너가 이 request-scoped
 * 빈에 결과를 적재하고 컨트롤러가 서비스 호출 후 읽어 piggyback 응답을 조립한다.
 */
@Component
@RequestScope
public class EarnedBadgeCollector {

    private final List<Badge> earned = new ArrayList<>();

    public void add(Badge badge) {
        earned.add(badge);
    }

    public List<Badge> getEarned() {
        return List.copyOf(earned);
    }
}
