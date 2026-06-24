const CUTOFF_HOUR = 4

// 새벽 04:00 이전이면 전날로 본다 (백엔드 EffectiveDateResolver와 동일한 규칙).
export function getEffectiveToday() {
  const now = new Date()
  if (now.getHours() < CUTOFF_HOUR) {
    now.setDate(now.getDate() - 1)
  }
  return now
}
