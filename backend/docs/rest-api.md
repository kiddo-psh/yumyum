# REST API Rules

- API 버전 관리 없음. Breaking change 시 클라이언트와 동시 배포한다.
- 날짜: `YYYY-MM-DD`, 날짜+시간: ISO 8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`)
- 빈 목록은 `null` 대신 `[]` 반환.

## URI 설계

- 경로 변수는 리소스 식별자에만 사용. (예: `/meals/{mealId}`)
- 필터 조건은 쿼리 파라미터. (예: `/meals?date=2026-06-01`)
- 중첩 URI는 1단계까지만 허용. (예: `/meals/{mealId}/items`)

## Controller 응답

- 항상 `ResponseEntity` 반환. `ResponseEntity<?>` 사용 금지.
- 201 Created 시 `Location` 헤더에 생성 URI 포함.
- body 없음: `ResponseEntity<Void>`.
- 성공 응답은 리소스를 그대로 반환 (`{"success": true, "data": ...}` 래핑 금지).

## 에러 처리

`@ControllerAdvice`로 전역 처리. 개별 Controller에서 직접 처리 금지.

```json
{ "code": "ERROR_CODE", "message": "에러 메시지" }
```

| HTTP | 코드 | 설명 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 요청 파라미터 유효성 오류 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `FORBIDDEN` | 권한 없음 (타인 리소스 접근) |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복 데이터 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

## 페이지네이션

검색성 목록 API에만 적용: `?page=0&size=20`. 날짜 기반 조회 등 결과가 제한적인 API에는 적용하지 않는다.
