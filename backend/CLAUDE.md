# CLAUDE.md

이 파일은 Claude Code가 이 프로젝트에서 작업할 때 참조하는 코딩 컨벤션과 가이드라인을 담고 있습니다.

## Backend (Spring Boot)

### REST API 공통 규칙

- 모든 API 경로는 `/api` 접두사로 시작한다. (예: `/api/meals`, `/api/programs`)
- API 버전 관리는 하지 않는다. Breaking change 시 클라이언트와 동시 배포한다.
- 날짜 형식: `YYYY-MM-DD`, 날짜+시간 형식: ISO 8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`)
- 빈 목록 응답은 `null` 대신 `[]`를 반환한다.

### URI 설계

- 경로 변수는 리소스 식별자에만 사용한다. (예: `/meals/{mealId}`)
- 필터 조건은 쿼리 파라미터로 표현한다. (예: `/meals?date=2026-06-01`)
- 종속 리소스는 중첩 URI로 표현하되, 1단계까지만 허용한다. (예: `/meals/{mealId}/items`)

### HTTP 메서드

- `POST`: 리소스 생성
- `GET`: 리소스 조회
- `PUT`: 리소스 전체 교체
- `PATCH`: 리소스 부분 수정 (전송한 필드만 변경)
- `DELETE`: 리소스 삭제

### Controller 응답 규칙

- Controller는 항상 `ResponseEntity`를 반환한다.
- 자원을 생성(201 Created)하는 경우 `Location` 헤더에 생성된 자원의 URI를 포함한다.
- 응답 body가 없는 경우 반환형은 `ResponseEntity<Void>`로 명시한다.
- `ResponseEntity<?>`는 직관적이지 않으므로 사용하지 않는다.
- 성공 응답은 리소스를 그대로 반환한다. `{"success": true, "data": ...}` 형태로 래핑하지 않는다.

### 에러 처리

- 에러 처리는 `@ControllerAdvice`로 전역에서 처리한다. 개별 Controller에서 에러를 직접 처리하지 않는다.
- 에러 응답 body 형식:
  ```json
  {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
  ```
- 공통 에러 코드:

  | HTTP | 코드 | 설명 |
  |---|---|---|
  | 400 | `VALIDATION_ERROR` | 요청 파라미터 유효성 오류 |
  | 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
  | 403 | `FORBIDDEN` | 권한 없음 (타인 리소스 접근) |
  | 404 | `NOT_FOUND` | 리소스 없음 |
  | 409 | `CONFLICT` | 중복 데이터 |
  | 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

### 페이지네이션

- 결과가 많아질 수 있는 검색성 목록 API에만 페이지네이션을 적용한다.
- 오프셋 기반 방식을 사용한다: `?page=0&size=20`
- 날짜 기반 조회 등 결과가 제한적인 API에는 페이지네이션을 적용하지 않는다.
