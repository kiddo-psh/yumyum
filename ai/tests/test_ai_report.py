import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

DAILY_NUTRITION = [
    {
        "date": f"2026-06-{13+i:02d}",
        "kcal": 1800.0 + i * 20,
        "protein_g": 130.0,
        "carb_g": 220.0,
        "fat_g": 55.0,
        "calories_burned": 200.0,
    }
    for i in range(7)
]
WEIGHT_RECORDS = [
    {"date": f"2026-06-{13+i:02d}", "weight_kg": 80.0 - i * (1/7)}
    for i in range(7)
]
WEEKLY_REPORT_REQUEST = {
    "program_id": 1,
    "week_number": 1,
    "health_goal": "DIET",
    "target_kcal": 2000.0,
    "target_protein_g": 150.0,
    "target_carb_g": 240.0,
    "target_fat_g": 60.0,
    "daily_nutrition": DAILY_NUTRITION,
    "weight_records": WEIGHT_RECORDS,
}


def test_주간_리포트_성공시_200과_구조_반환():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "avg_calorie_rate" in data
    assert "avg_protein_rate" in data
    assert "avg_carb_rate" in data
    assert "avg_fat_rate" in data
    assert "achievement_days" in data
    assert "weight_trend" in data
    assert "ai_comment" in data
    assert isinstance(data["ai_comment"], str)
    assert len(data["ai_comment"]) > 0


def test_달성률_평균이_올바르게_계산된다():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    data = response.json()
    # 첫날: (1800+200)/2000=100%, 마지막날: (1920+200)/2000=106%
    assert 95.0 <= data["avg_calorie_rate"] <= 115.0


def test_체중_기록_있으면_weight_trend_반환():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    data = response.json()
    assert data["weight_trend"] is not None
    assert data["weight_trend"] < 0   # 체중 감소 추세


def test_체중_기록_없으면_weight_trend_None():
    req = {**WEEKLY_REPORT_REQUEST, "weight_records": []}
    response = client.post("/ai/report/weekly", json=req)
    assert response.status_code == 200
    assert response.json()["weight_trend"] is None


def test_달성일수는_0이상_7이하():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    data = response.json()
    assert 0 <= data["achievement_days"] <= 7


# ── /ai/plan/weekly-adjust ───────────────────────────────────────────

WEEKLY_ADJUST_REQUEST = {
    "program_id": 1,
    "week_number": 1,
    "health_goal": "DIET",
    "sex": "FEMALE",
    "current_target_kcal": 1800.0,
    "weight_trend": 0.2,   # 감소 안 됨 → 칼로리 감량 필요
}


def test_칼로리_조정_성공시_200과_구조_반환():
    response = client.post("/ai/plan/weekly-adjust", json=WEEKLY_ADJUST_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "new_target_kcal" in data
    assert "adjustment" in data
    assert "reason" in data
    assert "ai_comment" in data
    assert isinstance(data["reason"], str)
    assert len(data["ai_comment"]) > 0


def test_DIET_감소_부족시_칼로리_감량():
    response = client.post("/ai/plan/weekly-adjust", json=WEEKLY_ADJUST_REQUEST)
    data = response.json()
    assert data["new_target_kcal"] == 1700.0
    assert data["adjustment"] == -100.0


def test_여성_칼로리_하한선_1200_보장():
    req = {**WEEKLY_ADJUST_REQUEST, "current_target_kcal": 1250.0, "sex": "FEMALE"}
    response = client.post("/ai/plan/weekly-adjust", json=req)
    assert response.json()["new_target_kcal"] >= 1200.0


def test_남성_칼로리_하한선_1500_보장():
    req = {**WEEKLY_ADJUST_REQUEST, "current_target_kcal": 1550.0, "sex": "MALE"}
    response = client.post("/ai/plan/weekly-adjust", json=req)
    assert response.json()["new_target_kcal"] >= 1500.0


def test_weight_trend_None이면_칼로리_유지():
    req = {**WEEKLY_ADJUST_REQUEST, "weight_trend": None}
    response = client.post("/ai/plan/weekly-adjust", json=req)
    data = response.json()
    assert data["new_target_kcal"] == WEEKLY_ADJUST_REQUEST["current_target_kcal"]
    assert data["adjustment"] == 0.0
