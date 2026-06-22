from app.schemas.coach import ExerciseCoachItem

_PRIORITY_ACTIONS = {"DOWN", "DELOAD"}


def pick_focus_exercise(exercises: list[ExerciseCoachItem]) -> str:
    """
    포커스 운동 선택.
    DOWN/DELOAD 액션 운동 우선, 없으면 성공률 최저 운동 반환.
    동점 시 성공률 기준 오름차순 min → 먼저 나오는 운동 선택.
    """
    if not exercises:
        raise ValueError("운동 목록이 비어 있습니다")
    priority = [e for e in exercises if e.last_action in _PRIORITY_ACTIONS]
    candidates = priority if priority else exercises
    return min(candidates, key=lambda e: e.success_rate).exercise_name
