# ADR: /ai/plan/generate에서 Claude 호출 제거 — 템플릿 코멘트로 대체

**Date:** 2026-06-24

## Context

`POST /ai/plan/generate`(온보딩 시 호출)는 BMR·TDEE 공식 계산 후 마지막에 Claude API로
"식단 시작 조언" 코멘트를 생성했다. 이 Claude 호출이 응답 전체를 3~5초 지연시켰다.

## Decision

`/ai/plan/generate`에서 Claude 호출을 완전히 제거하고, 건강 목표(WEIGHT_LOSS / MUSCLE_GAIN / MAINTAIN)별
정적 템플릿 문자열로 즉시 응답한다.

```python
_PLAN_COMMENTS = {
    "WEIGHT_LOSS": "하루 {kcal:.0f}kcal 목표로 시작해요! ...",
    "MUSCLE_GAIN": "근육 증가를 위해 하루 {kcal:.0f}kcal, ...",
    "MAINTAIN":    "건강 유지를 위한 하루 {kcal:.0f}kcal 플랜이 ...",
}
```

## Rationale

- **데이터 없는 시점에 AI 코멘트는 가치가 낮다.** 온보딩 직후엔 식단·운동 기록이 전혀 없어
  "개인화 조언"이 실질적으로 불가능하다. 목표별 일반 문장과 품질 차이가 없다.
- **3~5초 지연이 UX를 크게 해친다.** Program 생성이 `@Async`로 돌더라도 FastAPI 왕복 시간이
  홈화면 폴링 대기 시간을 결정한다. Claude 제거로 수십ms 수준으로 단축됐다.
- **Haiku가 이미 가장 빠른 모델이다.** Sonnet·Opus로 교체해도 더 느려진다. 모델 교체로는
  이 지연을 해결할 수 없다.
- **개인화 AI 코멘트는 데이터가 쌓인 후 의미 있다.** 홈화면 코멘트(12h Redis 캐시)와
  주간 리포트가 그 역할을 담당한다.

## Alternatives Considered

- **max_tokens 축소(1000→300)**: 1~2초 개선에 그침, 근본 해결 아님. 보조 조치로만 적용.
- **비동기 코멘트 생성 + 별도 저장**: 복잡도 증가, 코멘트 조회 엔드포인트 추가 필요. 가치 대비 공수 과다.
- **Streaming 응답**: Spring RestClient가 동기 방식이라 스트리밍 체인 구성이 복잡. 거부.

## Consequences

- `ai_comment` 필드는 계속 응답에 포함되며, 목표별 템플릿 문자열이 들어간다.
- 향후 주간 리포트 생성 시점(데이터 충분) 등에 Program의 `aiComment`를 Claude로 업데이트하는
  별도 배치를 추가할 수 있다.
- `claude_service.py` import가 `ai_plan.py`에서 제거됐다.
