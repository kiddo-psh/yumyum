# grant 결과는 request-scoped 수집기로 piggyback 응답에 싣는다

뱃지 획득(grant)은 트랜잭션 안 `@EventListener`에서 일어나므로 값을 호출자에게 반환하지 않는다. 그런데 "방금 획득한 뱃지 + 스트릭 변화"는 기록 API(`POST /meals`, `POST /routines/sessions`)의 응답에 함께 실어 보낸다(piggyback). 이 간극을 메우기 위해 `@RequestScope` 수집기 빈을 둔다: 동기 이벤트는 같은 요청·같은 스레드에서 돌기 때문에, 리스너의 grant와 StreakListener가 이 수집기에 결과를 적재하고 컨트롤러가 서비스 호출 후 읽어 응답을 조립한다.

## 고려한 대안

- **직접 반환**: `MealService`가 뱃지 평가를 직접 호출해 결과를 반환. 데이터 흐름은 명시적이지만 nutrition/exercise가 growth를 직접 알게 되어 [ADR-0001](./0001-growth-reacts-to-domain-events.md)의 이벤트 경계 결정을 되돌린다. 그래서 채택하지 않음.

## 결과

- 데이터가 명시적 메서드 반환이 아니라 공유 빈을 통해 흐른다(추적 비용 ↑). 대신 이벤트 디커플링이 유지된다.
- piggyback이므로 "안 본 뱃지(seen/unseen)" 플래그는 필요 없다 — 응답은 *이번 요청에서* 획득된 것만 담는다. 미접속 중 다른 경로로 획득하는 경우가 생기면 그때 별도 조회 메커니즘을 재검토한다.
