from typing import List, Tuple
from app.schemas.routine import RecentSessionData


def calculate_adjustment(
    exercise_id: int,
    target_sets: int,
    target_reps: int,
    target_weight_kg: float,
    recent_sessions: List[RecentSessionData],
) -> Tuple[str, float, int, int]:
    """
    계산 루틴 조정: 세션 성공률 기반 운동 강도 조정

    Returns:
        (action, new_weight_kg, new_sets, new_reps)
        action: "UP" | "HOLD" | "DOWN" | "VOLUME_UP" | "DELOAD"
    """
    # 해당 운동의 세션만 필터링
    exercise_sessions = []
    for session in recent_sessions:
        for s in session.sets:
            if s.exercise_id == exercise_id:
                exercise_sessions.append(s)
                break

    # 세션이 없으면 HOLD
    if not exercise_sessions:
        return "HOLD", target_weight_kg, target_sets, target_reps

    # 성공률 계산 (completed / target)
    rates = [s.actual_sets_completed / s.target_sets for s in exercise_sessions]

    # DELOAD: 4주 누적 피로 감지 (처음보다 40% 이상 감소 & 마지막 성공률 < 60%)
    if len(rates) >= 4 and rates[-1] < rates[0] * 0.6 and rates[-1] < 0.6:
        return "DELOAD", round(target_weight_kg * 0.6, 1), max(1, target_sets - 1), target_reps

    # 연속 성공/실패 확인
    consecutive_success = 0
    consecutive_failure = 0
    for rate in reversed(rates):
        if rate >= 1.0:
            if consecutive_failure > 0:
                break
            consecutive_success += 1
        elif rate < 0.5:
            if consecutive_success > 0:
                break
            consecutive_failure += 1
        else:
            break

    # VOLUME_UP: 3회 연속 성공 (100% 이상)
    if consecutive_success >= 3:
        return "VOLUME_UP", round(target_weight_kg + 5.0, 1), target_sets + 1, target_reps

    # DOWN: 2회 연속 실패 (50% 미만)
    if consecutive_failure >= 2:
        return "DOWN", round(target_weight_kg * 0.9, 1), target_sets, target_reps

    # UP: 마지막 세션 전성공 (100% 이상)
    if rates[-1] >= 1.0:
        increment = 2.5 if target_weight_kg < 30 else 5.0
        return "UP", round(target_weight_kg + increment, 1), target_sets, target_reps

    # 기본값: HOLD
    return "HOLD", target_weight_kg, target_sets, target_reps
