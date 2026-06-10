# ai/tests/test_ai_routine_adjust.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "routine_id": 1,
    "current_week_number": 1,
    "exercises": [
        {"exercise_id": 1, "day_label": "상체", "exercise_name": "벤치프레스",
         "target_sets": 4, "target_reps": 8, "target_weight_kg": 60.0, "order_index": 0}
    ],
    "recent_sessions": [
        {"session_date": "2026-06-03", "sets": [
            {"exercise_id": 1, "exercise_name": "벤치프레스",
             "target_sets": 4, "actual_sets_completed": 4,
             "avg_actual_reps": 8.0, "avg_actual_weight_kg": 60.0}
        ]}
    ]
}


def test_adjust_성공시_200과_유효한_구조_반환():
    response = client.post("/ai/routine/adjust", json=VALID_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "adjustments" in data
    assert "ai_comment" in data
    assert data["next_week_number"] == 2
    assert len(data["adjustments"]) == 1
    adj = data["adjustments"][0]
    assert adj["exercise_id"] == 1
    assert adj["action"] in {"UP", "HOLD", "DOWN", "VOLUME_UP", "DELOAD"}
    assert "new_weight_kg" in adj
    assert "new_sets" in adj
    assert "new_reps" in adj
    assert "reason" in adj


def test_세션없으면_HOLD_반환():
    req = {**VALID_REQUEST, "recent_sessions": []}
    response = client.post("/ai/routine/adjust", json=req)
    assert response.status_code == 200
    assert response.json()["adjustments"][0]["action"] == "HOLD"
