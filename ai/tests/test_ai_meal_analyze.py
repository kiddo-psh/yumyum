import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "total_kcal": 1500.0,
    "total_protein_g": 100.0,
    "total_carb_g": 180.0,
    "total_fat_g": 50.0,
    "target_kcal": 2000.0,
    "target_protein_g": 150.0,
    "target_carb_g": 250.0,
    "target_fat_g": 60.0,
    "health_goal": "MUSCLE",
    "meal_date": "2026-06-15",
}


def test_분석_성공시_200과_유효한_구조_반환():
    response = client.post("/ai/meal/diet-analyze", json=VALID_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "calorie_rate" in data
    assert "protein_rate" in data
    assert "carb_rate" in data
    assert "fat_rate" in data
    assert "balance_score" in data
    assert "weak_nutrients" in data
    assert "excess_nutrients" in data
    assert "ai_comment" in data
    assert isinstance(data["weak_nutrients"], list)
    assert isinstance(data["excess_nutrients"], list)
    assert isinstance(data["ai_comment"], str)
    assert len(data["ai_comment"]) > 0


def test_달성률이_응답에_정확히_반영된다():
    response = client.post("/ai/meal/diet-analyze", json=VALID_REQUEST)
    data = response.json()
    assert data["calorie_rate"] == 75.0


def test_부족_영양소가_weak_nutrients에_포함된다():
    response = client.post("/ai/meal/diet-analyze", json=VALID_REQUEST)
    data = response.json()
    assert "protein" in data["weak_nutrients"]


def test_목표치_초과시_excess_nutrients에_포함된다():
    req = {**VALID_REQUEST, "total_fat_g": 80.0, "target_fat_g": 60.0}
    response = client.post("/ai/meal/diet-analyze", json=req)
    data = response.json()
    assert "fat" in data["excess_nutrients"]


def test_모든_목표_달성시_weak_excess_모두_빈리스트():
    perfect = {
        **VALID_REQUEST,
        "total_kcal": 2000.0, "total_protein_g": 150.0,
        "total_carb_g": 250.0, "total_fat_g": 60.0,
    }
    response = client.post("/ai/meal/diet-analyze", json=perfect)
    data = response.json()
    assert data["weak_nutrients"] == []
    assert data["excess_nutrients"] == []
    assert data["balance_score"] == 100.0
