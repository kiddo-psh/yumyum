# ai/tests/test_ai_routine.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "gender": "M",
    "age": 25,
    "weight_kg": 75.0,
    "height_cm": 178.0,
    "health_goal": "MUSCLE_GAIN",
    "days_per_week": 4,
    "split_type": "UPPER_LOWER_4",
    "split_labels": ["상체", "하체", "상체", "하체"]
}


def test_루틴_생성_성공시_200과_유효한_구조_반환():
    response = client.post("/ai/routine/generate", json=VALID_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "routine_name" in data
    assert "days" in data
    assert len(data["days"]) == 4
    assert "ai_comment" in data
    for day in data["days"]:
        assert "day_label" in day
        assert len(day["exercises"]) > 0
        for ex in day["exercises"]:
            assert "name" in ex
            assert "sets" in ex
            assert "reps" in ex
            assert "weight_kg" in ex


def test_split_labels_개수가_days_per_week와_일치한다():
    req = {**VALID_REQUEST, "days_per_week": 3, "split_labels": ["상체", "하체", "전신"]}
    response = client.post("/ai/routine/generate", json=req)
    assert response.status_code == 200
