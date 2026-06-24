# MyView.vue 문법 오류 — if 블록을 bare `{` 로 교체해 orphan else 발생

**Date:** 2026-06-24
**Time spent:** ~5분

## Symptom

```
[plugin:vite:vue] [vue/compiler-sfc] Unexpected token (43:4)
MyView.vue:43:4
41 |
42 | onMounted(async () => {
43 |   // 프로필 먼저 로딩 (memberId가 Program 조회에 필요)
   |                                   ^
```

Vite dev 서버가 `MyView.vue`를 파싱하지 못해 화면 전체가 오버레이 에러로 막힘.

## Wrong tracks

없음 — 오류 메시지가 파일·라인을 명확히 가리켰다.

## Root cause

`getCurrentProgram` 호출부를 감싸던 `if (profile.value?.memberId) { ... } else { ... }` 블록에서
`if` 조건문을 제거해 bare `{`만 남겼는데, 대응하는 `else { ... }` 절이 그대로 남아 문법 오류 발생.

변경 전:
```js
if (profile.value?.memberId) {
  const programResult = await Promise.allSettled([getCurrentProgram(profile.value.memberId)]);
  ...
} else {
  programError.value = new Error('...')
}
```

변경 후 (잘못됨):
```js
{
  const programResult = await Promise.allSettled([getCurrentProgram()]);
  ...
} else {   // ← orphan else: 문법 오류
  programError.value = ...
}
```

## Fix

`{...} else {...}` 전체를 단순 순차 코드로 교체. JWT 인증 덕분에 `memberId` 조건 자체가 불필요해짐.

```js
const programResult = await Promise.allSettled([getCurrentProgram()])
if (programResult[0].status === 'fulfilled') {
  program.value = programResult[0].value
} else {
  programError.value = programResult[0].reason
}
```

## Prevention

- `if` 조건만 제거하고 `else` 절을 남기지 않도록, if 블록 전체를 한 번에 치환한다.
- Edit 툴 사용 시 `old_string`에 `if (...) {` ~ 마지막 `}` 까지 포함시켜 교체하는 것이 안전하다.
