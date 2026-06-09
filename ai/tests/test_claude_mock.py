import os
os.environ.setdefault("ENV", "dev")

from app.services.claude_service import _mock_response


def test_루틴_프롬프트는_JSON_루틴을_반환한다():
    import json
    result = _mock_response("주 4회 루틴을 JSON으로 생성해주세요")
    data = json.loads(result)
    assert "routine_name" in data
    assert "days" in data
    assert len(data["days"]) > 0
    assert "ai_comment" in data


def test_식단_프롬프트는_기존_포맷을_유지한다():
    import json
    result = _mock_response("JSON 식단 추천해줘")
    items = json.loads(result)
    assert isinstance(items, list)
    assert "name" in items[0]
