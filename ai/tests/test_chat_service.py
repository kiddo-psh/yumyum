from app.services.chat_service import build_prompt
from app.schemas.chat import MealContext


def _make_context(**kwargs) -> MealContext:
    defaults = dict(
        total_protein_g=39.0, target_protein_g=120.0,
        total_carb_g=150.0,  target_carb_g=250.0,
        total_fat_g=30.0,    target_fat_g=60.0,
        total_kcal=900.0,    target_kcal=2000.0,
        health_goal="MUSCLE",
    )
    defaults.update(kwargs)
    return MealContext(**defaults)


def test_prompt_contains_message():
    prompt = build_prompt("단백질 많은 음식 뭐야?", docs=[], context=None)
    assert "단백질 많은 음식 뭐야?" in prompt


def test_prompt_contains_doc_names():
    docs = [
        {"name": "닭가슴살", "info": "100g당 단백질 23g", "document": "..."},
        {"name": "두부",     "info": "100g당 단백질 8g",  "document": "..."},
    ]
    prompt = build_prompt("단백질", docs=docs, context=None)
    assert "닭가슴살" in prompt
    assert "두부" in prompt


def test_prompt_contains_context_values():
    ctx = _make_context(total_protein_g=39.0, target_protein_g=120.0)
    prompt = build_prompt("오늘 단백질 충분한가요?", docs=[], context=ctx)
    assert "39" in prompt
    assert "120" in prompt


def test_prompt_health_goal_translated_to_korean():
    ctx = _make_context(health_goal="DIET")
    prompt = build_prompt("질문", docs=[], context=ctx)
    assert "다이어트" in prompt


def test_prompt_no_context_section_when_context_is_none():
    prompt = build_prompt("닭가슴살 칼로리?", docs=[], context=None)
    assert "사용자 오늘 식단" not in prompt


def test_prompt_no_docs_section_when_docs_empty():
    prompt = build_prompt("질문", docs=[], context=None)
    assert "참고 식품 정보" not in prompt


def test_prompt_all_health_goals_translate():
    for goal, expected in [
        ("DIET",    "다이어트"),
        ("MUSCLE",  "근육 증가"),
        ("HEALTH",  "건강 유지"),
        ("DISEASE", "질환 관리"),
    ]:
        ctx = _make_context(health_goal=goal)
        prompt = build_prompt("질문", docs=[], context=ctx)
        assert expected in prompt, f"{goal} → {expected} 번역 실패"
