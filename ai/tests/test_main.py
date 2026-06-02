import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health():
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"


def test_last_recommend_basic():
    payload = {
        "total_kcal": 1300,
        "total_protein_g": 50,
        "total_carb_g": 160,
        "total_fat_g": 35,
        "target_kcal": 2000,
        "target_protein_g": 150,
        "target_carb_g": 225,
        "target_fat_g": 55,
        "meal_count": 2,
    }
    res = client.post("/ai/meal/last-recommend", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert len(body["recommendations"]) == 3
    assert body["priority_nutrient"] in ("protein", "carb", "fat")


def test_last_recommend_no_meal():
    """끼니 기록 없으면 400 반환"""
    payload = {
        "total_kcal": 0, "total_protein_g": 0, "total_carb_g": 0, "total_fat_g": 0,
        "target_kcal": 2000, "target_protein_g": 150, "target_carb_g": 225, "target_fat_g": 55,
        "meal_count": 0,
    }
    res = client.post("/ai/meal/last-recommend", json=payload)
    assert res.status_code == 400


def test_plan_generate():
    payload = {
        "gender": "M",
        "age": 28,
        "height_cm": 175,
        "weight_kg": 75,
        "activity_level": "moderate",
        "health_goal": "MUSCLE_GAIN",
    }
    res = client.post("/ai/plan/generate", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert body["target_kcal"] >= 1500   # 안전 하한선 이상
    assert body["bmr"] > 0


# ── Food Search ──────────────────────────────────────────────────────────────
def test_food_search_basic():
    """mock DB에 있는 키워드 검색 — 결과 반환 확인"""
    res = client.get("/food/search", params={"query": "닭가슴살"})
    assert res.status_code == 200
    body = res.json()
    assert body["query"] == "닭가슴살"
    assert body["page"] == 1
    assert body["size"] == 10
    assert isinstance(body["items"], list)
    assert body["total"] >= 1

    first = body["items"][0]
    assert first["kcal"] > 0
    assert first["protein_g"] >= 0
    assert first["carb_g"] >= 0
    assert first["fat_g"] >= 0
    assert first["serving_size_g"] > 0
    assert first["source"] in ("MFDS", "AI")
    assert "food_cd" in first
    assert "name" in first


def test_food_search_response_schema():
    """응답 필드 전체 구조 검증"""
    res = client.get("/food/search", params={"query": "계란"})
    assert res.status_code == 200
    body = res.json()

    required_top = {"items", "total", "page", "size", "query"}
    assert required_top <= body.keys()

    required_item = {"food_cd", "name", "serving_size_g", "kcal", "protein_g", "carb_g", "fat_g", "source"}
    for item in body["items"]:
        assert required_item <= item.keys(), f"필드 누락: {required_item - item.keys()}"


def test_food_search_pagination():
    """page/size 파라미터 동작 검증"""
    res = client.get("/food/search", params={"query": "닭", "page": 1, "size": 1})
    assert res.status_code == 200
    body = res.json()
    assert len(body["items"]) <= 1
    assert body["size"] == 1
    assert body["page"] == 1


def test_food_search_empty_query():
    """빈 검색어는 422 반환"""
    res = client.get("/food/search", params={"query": ""})
    assert res.status_code == 422


def test_food_search_size_limit():
    """size > 50 은 422 반환"""
    res = client.get("/food/search", params={"query": "닭", "size": 99})
    assert res.status_code == 422


def test_food_search_no_results_returns_empty_or_ai():
    """mock DB에 없는 검색어 — 빈 리스트 또는 AI fallback 반환 (500 아님)"""
    res = client.get("/food/search", params={"query": "xyzxyz알수없는식품"})
    assert res.status_code == 200
    body = res.json()
    # AI fallback 또는 빈 결과 — 어느 쪽이든 서버 오류 없이 200 반환
    assert isinstance(body["items"], list)
    for item in body["items"]:
        assert item["source"] == "AI"
