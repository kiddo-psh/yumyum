import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, AsyncMock
from app.main import app

client = TestClient(app)


def test_chat_mock_mode_returns_200():
    """ENV=dev(기본값)일 때 mock 응답 반환."""
    resp = client.post("/ai/chat", json={"message": "단백질 많은 음식 추천해줘"})
    assert resp.status_code == 200
    body = resp.json()
    assert "answer" in body
    assert isinstance(body["sources"], list)


def test_chat_mock_response_contains_mock_marker():
    resp = client.post("/ai/chat", json={"message": "아무거나"})
    assert "[MOCK]" in resp.json()["answer"]


def test_chat_with_context_mock_mode():
    payload = {
        "message": "오늘 단백질 충분한가요?",
        "context": {
            "total_protein_g": 39.0,
            "target_protein_g": 120.0,
            "total_carb_g": 150.0,
            "target_carb_g": 250.0,
            "total_fat_g": 30.0,
            "target_fat_g": 60.0,
            "total_kcal": 900.0,
            "target_kcal": 2000.0,
            "health_goal": "MUSCLE",
        },
    }
    resp = client.post("/ai/chat", json=payload)
    assert resp.status_code == 200
    body = resp.json()
    assert "answer" in body
    assert "sources" in body


def test_chat_missing_message_returns_422():
    resp = client.post("/ai/chat", json={})
    assert resp.status_code == 422


def test_chat_sources_have_name_and_info():
    resp = client.post("/ai/chat", json={"message": "test"})
    sources = resp.json()["sources"]
    for s in sources:
        assert "name" in s
        assert "info" in s


def test_chat_prod_mode_calls_rag_and_claude():
    """ENV=prod 시 rag_service.search와 call_claude가 호출된다."""
    with patch("app.routers.ai_chat.settings") as mock_settings, \
         patch("app.routers.ai_chat.rag_service.search", return_value=[
             {"name": "닭가슴살", "info": "100g당 단백질 23g", "document": "..."}
         ]) as mock_search, \
         patch("app.routers.ai_chat.call_claude", new_callable=AsyncMock,
               return_value="닭가슴살을 드세요!") as mock_claude:
        mock_settings.env = "prod"
        resp = client.post("/ai/chat", json={"message": "단백질 음식 추천"})

    assert resp.status_code == 200
    mock_search.assert_called_once_with("단백질 음식 추천")
    mock_claude.assert_called_once()
