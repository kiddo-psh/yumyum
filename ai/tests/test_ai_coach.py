import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

_EXERCISES = [
    {
        "exercise_name": "스쿼트",
        "current_weight_kg": 80.0,
        "current_sets": 4,
        "current_reps": 8,
        "last_action": "DOWN",
        "success_rate": 0.4,
    },
    {
        "exercise_name": "벤치프레스",
        "current_weight_kg": 60.0,
        "current_sets": 4,
        "current_reps": 8,
        "last_action": "HOLD",
        "success_rate": 0.8,
    },
    {
        "exercise_name": "데드리프트",
        "current_weight_kg": 100.0,
        "current_sets": 3,
        "current_reps": 5,
        "last_action": "UP",
        "success_rate": 1.0,
    },
]

COACH_REQUEST = {
    "health_goal": "MUSCLE",
    "week_number": 3,
    "exercises": _EXERCISES,
}


def test_코치_성공시_200과_구조_반환():
    response = client.post("/ai/exercise/coach", json=COACH_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "focus_exercise" in data
    assert "overall_comment" in data
    assert isinstance(data["focus_exercise"], str)
    assert len(data["overall_comment"]) > 0


def test_DOWN_운동이_focus_exercise로_선택됨():
    response = client.post("/ai/exercise/coach", json=COACH_REQUEST)
    assert response.json()["focus_exercise"] == "스쿼트"


def test_DOWN_없으면_성공률_최저_운동이_focus():
    req = {
        **COACH_REQUEST,
        "exercises": [
            {**_EXERCISES[1], "last_action": "HOLD", "success_rate": 0.5},
            {**_EXERCISES[2], "last_action": "HOLD", "success_rate": 0.9},
        ],
    }
    response = client.post("/ai/exercise/coach", json=req)
    assert response.json()["focus_exercise"] == "벤치프레스"


def test_운동_목록_빈_경우_422():
    req = {**COACH_REQUEST, "exercises": []}
    response = client.post("/ai/exercise/coach", json=req)
    # Field(min_length=1) → Pydantic catches this before router → 422
    assert response.status_code == 422
