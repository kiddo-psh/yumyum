# ADR: My Page 구현 설계 결정 3건
**Date:** 2026-06-23

---

## ADR-1: 프로필 수정에 PUT 대신 PATCH /members/me 재사용

### Context
My Page 프로필 수정 기능 구현 시 엔드포인트 선택 필요.
기존 `PATCH /members/me` (부분 수정)가 이미 존재했고, 수정 가능한 필드 목록이 동일했음.

### Decision
새 `PUT /members/me`를 추가하지 않고, 기존 `PATCH /members/me`를 그대로 재사용.

### Rationale
- 수정 대상 필드가 완전히 동일 — 새 엔드포인트를 만들 이유가 없음
- `PUT`은 리소스 전체 교체를 의미하므로 의미론적으로 부적합 (일부 필드는 수정 불가 보호 필요)
- 사이드이펙트 없음: 기존 동작에 영향 없이 Vue에서 같은 엔드포인트를 호출하면 됨
- 엔드포인트 수 증가를 억제 → 유지보수 부담 감소

### Alternatives Considered
- **PUT /members/me 신규 추가:** 필드가 동일하므로 중복 코드 발생, 의미론적으로도 부정확
- **별도 /members/me/profile 경로:** 오버엔지니어링, 기존 Spring 컨트롤러 구조와 불일치

### Consequences
- 클라이언트(Vue)는 PATCH semantics로 호출 — 빈 필드를 보내지 않아야 함
- 향후 수정 가능 필드가 분기될 경우 엔드포인트 분리 재검토 필요

---

## ADR-2: 클래스 레벨 @RequestMapping("/growth") 제거, 메서드에 전체 경로 명시

### Context
`NyamController`(또는 관련 컨트롤러)에 클래스 레벨 `@RequestMapping("/growth")`가 선언되어 있었음.
`/nyam/status` 엔드포인트를 추가했을 때 실제 경로가 `/growth/nyam/status`로 매핑됨 (트러블슈팅 로그 참조).

### Decision
클래스 레벨 `@RequestMapping("/growth")` 제거.
각 메서드에 `/nyam/status`, `/nyam/...` 전체 경로를 직접 명시.

### Rationale
- Nyam 관련 엔드포인트는 `/nyam/*` 경로가 의미상 올바름 — growth 접두사는 다른 도메인 맥락
- 클래스 레벨 prefix가 있으면 메서드 경로만 봐서는 실제 URL을 알 수 없어 가독성 저하
- 전체 경로 명시 시 IDE에서 URL 검색이 가능해짐
- 동일 컨트롤러에 `/growth/*`와 `/nyam/*`가 혼재하는 상황 자체가 관심사 분리 위반

### Alternatives Considered
- **클래스 경로를 `/nyam`으로 변경:** 기존 `/growth/*` 엔드포인트가 있다면 하위 호환 파괴
- **컨트롤러 분리 (GrowthController / NyamController):** 향후 리팩터링 방향으로 유효하나, 지금 범위를 벗어남

### Consequences
- 향후 이 컨트롤러에 메서드 추가 시 전체 경로를 명시해야 함 (암묵적 prefix 없음)
- URL 오류 재발 방지: 경로가 코드에 명확히 드러남

---

## ADR-3: MyView 데이터 로딩에 Promise.all 대신 Promise.allSettled 사용

### Context
My Page(MyView)는 여러 API를 병렬 호출해 데이터를 조합함.
API 중 하나가 실패할 경우의 처리 전략 결정 필요.

### Decision
`Promise.all` 대신 `Promise.allSettled` 사용.

### Rationale
- `Promise.all`은 하나라도 reject되면 전체가 실패 → 나머지 정상 데이터도 화면에 표시 불가
- My Page는 독립적인 위젯(프로필 / Nyam 상태 / Streak 등)으로 구성 — 한 위젯 실패가 다른 위젯을 차단하면 안 됨
- `allSettled`는 각 결과를 `{status, value/reason}`으로 받으므로 부분 성공 렌더링 가능
- 사용자 경험: 네트워크 불안정 시 일부 영역만 에러 표시, 나머지는 정상 표시

### Alternatives Considered
- **Promise.all + try/catch 전체 묶기:** 실패 시 페이지 전체 에러 처리 — UX 열악
- **순차 await:** 병렬 이점 없음, 로딩 시간 누적

### Consequences
- 각 API 결과에 대해 `status === 'fulfilled'` 분기 처리 코드가 추가됨
- 에러 상태는 위젯별로 독립 관리 (전역 에러 상태와 혼용 금지)

---

## ADR-4: MyView에 Pinia 스토어 미추가, 로컬 컴포넌트 상태 사용

### Context
My Page 데이터(프로필, Nyam 상태, Streak)를 어디서 관리할지 결정 필요.

### Decision
Pinia 스토어를 새로 만들지 않고, MyView 컴포넌트 내부 로컬 상태(`ref`/`reactive`)로 관리.

### Rationale
- My Page 데이터는 다른 뷰에서 공유되지 않음 — 전역 상태로 올릴 이유 없음
- Pinia 스토어 추가 시 `state / getters / actions` + 초기화 타이밍 관리 비용 발생
- 페이지 진입마다 최신 데이터를 API로 새로 가져오므로 캐시 필요성도 낮음
- CLAUDE.md 원칙: "필요한 레이어만 추가" — 불필요한 스토어 생성 금지

### Alternatives Considered
- **myPageStore 신규 생성:** 다른 컴포넌트에서 참조할 필요가 생길 경우를 대비한 선제적 추가 — YAGNI
- **기존 memberStore 확장:** member 도메인을 벗어난 Nyam·Streak 데이터가 섞여 관심사 오염

### Consequences
- 향후 My Page 데이터를 다른 뷰에서 참조해야 하는 요구가 생기면 스토어로 이관 필요
- 현재 범위에서는 컴포넌트 코드가 단순하고 테스트가 쉬움
