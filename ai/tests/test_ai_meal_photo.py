import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "image_base64": "dGVzdA==",
    "media_type": "image/jpeg",
    "meal_type": "LUNCH",
}


def test_사진_분석_200_반환():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    assert response.status_code == 200


def test_사진_분석_응답_구조_검증():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    data = response.json()
    assert "detected_items" in data
    assert "total_kcal" in data
    assert "ai_comment" in data
    assert isinstance(data["detected_items"], list)


def test_detected_item_필드_검증():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    items = response.json()["detected_items"]
    assert len(items) > 0
    item = items[0]
    for field in ["name", "estimated_grams", "kcal", "protein_g", "carb_g", "fat_g"]:
        assert field in item


def test_total_kcal_아이템_합산과_일치():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    data = response.json()
    items_sum = sum(i["kcal"] for i in data["detected_items"])
    assert abs(data["total_kcal"] - items_sum) < 1.0


def test_빈_base64는_mock_모드에서도_정상_처리():
    """dev 환경에서는 base64 값과 무관하게 mock 응답 반환"""
    response = client.post("/ai/meal/analyze-photo", json={
        "image_base64": "",
        "media_type": "image/jpeg",
        "meal_type": "BREAKFAST",
    })
    assert response.status_code == 200
