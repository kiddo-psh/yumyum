import pytest
from app.services.coach_service import pick_focus_exercise
from app.schemas.coach import ExerciseCoachItem


def _item(name: str, last_action: str, success_rate: float) -> ExerciseCoachItem:
    return ExerciseCoachItem(
        exercise_name=name,
        current_weight_kg=60.0,
        current_sets=3,
        current_reps=10,
        last_action=last_action,
        success_rate=success_rate,
    )


def test_DOWN_액션_운동_우선_선택():
    exercises = [
        _item("스쿼트",     "DOWN", 0.4),
        _item("벤치프레스", "HOLD", 0.9),
        _item("데드리프트", "UP",   1.0),
    ]
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_DELOAD_액션_운동_우선_선택():
    exercises = [
        _item("스쿼트",     "DELOAD", 0.5),
        _item("벤치프레스", "UP",     1.0),
    ]
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_DOWN_DELOAD_없으면_성공률_최저_운동_선택():
    exercises = [
        _item("스쿼트",     "HOLD", 0.9),
        _item("벤치프레스", "UP",   0.6),
        _item("데드리프트", "HOLD", 0.8),
    ]
    assert pick_focus_exercise(exercises) == "벤치프레스"


def test_DOWN_여럿이면_성공률_낮은쪽_선택():
    exercises = [
        _item("스쿼트",     "DOWN", 0.3),
        _item("벤치프레스", "DOWN", 0.1),
        _item("데드리프트", "HOLD", 0.9),
    ]
    assert pick_focus_exercise(exercises) == "벤치프레스"


def test_DOWN과_DELOAD_동시_존재시_성공률_낮은쪽_선택():
    exercises = [
        _item("스쿼트",     "DOWN",   0.4),
        _item("벤치프레스", "DELOAD", 0.6),
    ]
    # 둘 다 priority_actions — 성공률 낮은 스쿼트 선택
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_단일_운동이면_그대로_반환():
    exercises = [_item("스쿼트", "UP", 1.0)]
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_빈_리스트면_ValueError():
    with pytest.raises(ValueError):
        pick_focus_exercise([])
