from app.services.routine_service import calculate_adjustment
from app.schemas.routine import SessionSetData, RecentSessionData


def _session(exercise_id: int, target: int, completed: int) -> RecentSessionData:
    return RecentSessionData(
        session_date="2026-06-01",
        sets=[SessionSetData(
            exercise_id=exercise_id, exercise_name="벤치프레스",
            target_sets=target, actual_sets_completed=completed,
            avg_actual_reps=8.0, avg_actual_weight_kg=60.0,
        )]
    )


def test_세션없으면_HOLD():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [])
    assert action == "HOLD"
    assert weight == 60.0
    assert sets == 4


def test_마지막_세션_전성공이면_UP():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [_session(1, 4, 4)])
    assert action == "UP"
    assert weight > 60.0


def test_마지막_세션_일부성공이면_HOLD():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [_session(1, 4, 2)])
    assert action == "HOLD"
    assert weight == 60.0


def test_2회_연속_실패면_DOWN():
    sessions = [_session(1, 4, 1), _session(1, 4, 1)]
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, sessions)
    assert action == "DOWN"
    assert weight < 60.0


def test_3회_연속_성공이면_VOLUME_UP():
    sessions = [_session(1, 4, 4), _session(1, 4, 4), _session(1, 4, 4)]
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, sessions)
    assert action == "VOLUME_UP"
    assert sets > 4


def test_4주_지속_감퇴면_DELOAD():
    sessions = [_session(1, 4, 4), _session(1, 4, 3), _session(1, 4, 2), _session(1, 4, 1)]
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, sessions)
    assert action == "DELOAD"
    assert weight < 60.0 * 0.7


def test_다른_운동_세션은_무시된다():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [_session(99, 4, 4)])
    assert action == "HOLD"
