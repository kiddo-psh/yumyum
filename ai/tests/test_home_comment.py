import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

BASE_REQUEST = {
    "member_id": 1,
    "health_goal": "MUSCLE",
    "current_streak": 5,
    "kcal_rate": 0.62,
    "remaining_kcal": 680.0,
    "protein_g": 45.0,
    "carb_g": 120.0,
    "fat_g": 28.0,
}


def test_홈_코멘트_200_반환():
    response = client.post("/ai/home/comment", json=BASE_REQUEST)
    assert response.status_code == 200


def test_응답에_comment_필드_존재():
    response = client.post("/ai/home/comment", json=BASE_REQUEST)
    data = response.json()
    assert "comment" in data
    assert isinstance(data["comment"], str)
    assert len(data["comment"]) > 0


def test_DIET_목표_mock_응답():
    req = {**BASE_REQUEST, "health_goal": "DIET", "current_streak": 0}
    response = client.post("/ai/home/comment", json=req)
    assert response.status_code == 200
    assert len(response.json()["comment"]) > 0


def test_DISEASE_목표_mock_응답():
    req = {**BASE_REQUEST, "health_goal": "DISEASE"}
    response = client.post("/ai/home/comment", json=req)
    assert response.status_code == 200


def test_streak_0일_때도_정상_반환():
    req = {**BASE_REQUEST, "current_streak": 0, "kcal_rate": 0.0}
    response = client.post("/ai/home/comment", json=req)
    assert response.status_code == 200
