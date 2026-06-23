import os
os.environ.setdefault("ENV", "dev")

import pytest
from app.services.claude_service import call_claude_vision


@pytest.mark.asyncio
async def test_vision_mock_모드에서_json_문자열_반환():
    result = await call_claude_vision(
        image_base64="dGVzdA==",
        media_type="image/jpeg",
        prompt="음식을 감지하세요",
    )
    assert isinstance(result, str)
    assert len(result) > 0


@pytest.mark.asyncio
async def test_vision_mock_응답에_detected_items_포함():
    import json
    result = await call_claude_vision(
        image_base64="dGVzdA==",
        media_type="image/jpeg",
        prompt="음식을 감지하세요",
    )
    data = json.loads(result)
    assert "detected_items" in data
    assert isinstance(data["detected_items"], list)
    assert len(data["detected_items"]) > 0


@pytest.mark.asyncio
async def test_vision_mock_각_아이템에_필수_필드_존재():
    import json
    result = await call_claude_vision(
        image_base64="dGVzdA==",
        media_type="image/jpeg",
        prompt="음식을 감지하세요",
    )
    data = json.loads(result)
    item = data["detected_items"][0]
    for field in ["name", "estimated_grams", "kcal", "protein_g", "carb_g", "fat_g"]:
        assert field in item, f"필드 누락: {field}"
