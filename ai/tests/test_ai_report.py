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
    expected = round(sum(
        (d["kcal"] + d["calories_burned"]) / WEEKLY_REPORT_REQUEST["target_kcal"] * 100
        for d in WEEKLY_REPORT_REQUEST["daily_nutrition"]
    ) / len(WEEKLY_REPORT_REQUEST["daily_nutrition"]), 1)
    assert data["avg_calorie_rate"] == expected


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


def test_달성일수_모든날_달성시_7():
    # 테스트 데이터: 모든 날이 칼로리 달성률 80~120% 범위
    # 최소: (1800+200)/2000=100%, 최대: (1920+200)/2000=106%
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    assert response.json()["achievement_days"] == 7


def test_달성일수_목표치_과도시_0():
    # target_kcal을 극단적으로 높게 설정해 모든 날 80% 미만으로 만듦
    req = {**WEEKLY_REPORT_REQUEST, "target_kcal": 5000.0}
    response = client.post("/ai/report/weekly", json=req)
    assert response.json()["achievement_days"] == 0


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


def test_MUSCLE_증가_속도_부족시_칼로리_증량():
    req = {**WEEKLY_ADJUST_REQUEST, "health_goal": "MUSCLE", "weight_trend": 0.05}
    response = client.post("/ai/plan/weekly-adjust", json=req)
    data = response.json()
    assert data["new_target_kcal"] == 1900.0
    assert data["adjustment"] == 100.0


def test_HEALTH_체중_안정시_칼로리_유지():
    req = {**WEEKLY_ADJUST_REQUEST, "health_goal": "HEALTH", "weight_trend": 0.3}
    response = client.post("/ai/plan/weekly-adjust", json=req)
    data = response.json()
    assert data["new_target_kcal"] == WEEKLY_ADJUST_REQUEST["current_target_kcal"]
    assert data["adjustment"] == 0.0


# ── /ai/checkin/biweekly ─────────────────────────────────────────────

BIWEEKLY_CHECKIN_REQUEST = {
    "program_id": 1,
    "week_number": 2,
    "health_goal": "DIET",
    "sex": "FEMALE",
    "achievement_rate": 35.7,   # 30~50% → LOW
    "current_target_kcal": 1800.0,
}


def test_2주_체크인_성공시_200과_구조_반환():
    response = client.post("/ai/checkin/biweekly", json=BIWEEKLY_CHECKIN_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "checkin_type" in data
    assert "adjustment_options" in data
    assert "ai_comment" in data
    assert isinstance(data["adjustment_options"], list)
    assert len(data["ai_comment"]) > 0


def test_달성률_30미만이면_VERY_LOW_타입():
    req = {**BIWEEKLY_CHECKIN_REQUEST, "achievement_rate": 20.0}
    response = client.post("/ai/checkin/biweekly", json=req)
    assert response.json()["checkin_type"] == "VERY_LOW"


def test_달성률_30이상이면_LOW_타입():
    req = {**BIWEEKLY_CHECKIN_REQUEST, "achievement_rate": 42.8}
    response = client.post("/ai/checkin/biweekly", json=req)
    assert response.json()["checkin_type"] == "LOW"


def test_DIET_목표시_옵션_3개_포함():
    response = client.post("/ai/checkin/biweekly", json=BIWEEKLY_CHECKIN_REQUEST)
    assert len(response.json()["adjustment_options"]) == 3


def test_HEALTH_목표시_옵션_2개_포함():
    req = {**BIWEEKLY_CHECKIN_REQUEST, "health_goal": "HEALTH"}
    response = client.post("/ai/checkin/biweekly", json=req)
    assert len(response.json()["adjustment_options"]) == 2


def test_KEEP_옵션이_항상_첫번째_포함():
    response = client.post("/ai/checkin/biweekly", json=BIWEEKLY_CHECKIN_REQUEST)
    options = response.json()["adjustment_options"]
    assert options[0]["option_id"] == "KEEP"
    assert options[0]["new_target_kcal"] == BIWEEKLY_CHECKIN_REQUEST["current_target_kcal"]
