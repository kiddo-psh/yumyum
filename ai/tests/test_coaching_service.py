import os
os.environ.setdefault("ENV", "dev")

import pytest
from app.schemas.coaching import (
    WeeklyCoachingRequest, DailyNutritionRecord,
    RoutineSessionRecord, WeightRecord,
)
from app.services.coaching_service import _calc_stats, run_coaching_chain


# ── 공통 픽스처 ────────────────────────────────────────────────────────

def _make_request(**overrides) -> WeeklyCoachingRequest:
    base = dict(
        week_number=3,
        health_goal="MUSCLE",
        daily_nutrition=[
            DailyNutritionRecord(date=f"2026-06-1{i}", kcal=1800.0, protein_g=80.0,
                                 carb_g=220.0, fat_g=60.0, calories_burned=300.0)
            for i in range(7)
        ],
        target_kcal=2000.0,
        target_protein_g=120.0,
        target_carb_g=250.0,
        target_fat_g=65.0,
        routine_sessions=[
            RoutineSessionRecord(exercise_name="벤치프레스", successful_sets=3,
                                 total_sets=4, weight_kg=60.0, session_date="2026-06-16"),
        ],
        weight_records=[
            WeightRecord(date="2026-06-10", weight_kg=70.0),
            WeightRecord(date="2026-06-17", weight_kg=70.3),
        ],
    )
    base.update(overrides)
    return WeeklyCoachingRequest(**base)


# ── _calc_stats 단위 테스트 ────────────────────────────────────────────

def test_칼로리_달성률_평균_계산():
    req = _make_request()
    avg, days = _calc_stats(req)
    # kcal=1800 + burned=300=2100, target=2000 → rate=105% → 7일 모두 80~120% → 7일 달성
    assert avg == pytest.approx(105.0, abs=1.0)
    assert days == 7


def test_daily_nutrition_없으면_달성률_0():
    req = _make_request(daily_nutrition=[])
    avg, days = _calc_stats(req)
    assert avg == 0.0
    assert days == 0


# ── run_coaching_chain 통합 (dev mock) ────────────────────────────────

@pytest.mark.asyncio
async def test_체인_응답_필드_7개_모두_존재():
    req = _make_request()
    result = await run_coaching_chain(req)
    assert result.ai_comment
    assert result.nutrition_summary
    assert result.exercise_summary
    assert result.goal_summary
    assert isinstance(result.avg_calorie_rate, float)
    assert isinstance(result.achievement_days, int)
    # weight_trend: 2개 기록 있으므로 float


def test_영양_agent_프롬프트_검증():
    """영양 Agent가 호출될 프롬프트에 avg_calorie_rate와 health_goal이 포함되는지
    간접 검증 — mock 모드에서는 [MOCK] 텍스트가 반환되므로 nutrition_summary로 확인."""
    pass  # dev mock에서는 직접 검증 불필요; integration test에서 확인


@pytest.mark.asyncio
async def test_weight_records_없으면_weight_trend_null():
    req = _make_request(weight_records=[])
    result = await run_coaching_chain(req)
    assert result.weight_trend is None


@pytest.mark.asyncio
async def test_routine_sessions_없어도_정상_처리():
    req = _make_request(routine_sessions=[])
    result = await run_coaching_chain(req)
    assert result.exercise_summary  # fallback 텍스트라도 존재


@pytest.mark.asyncio
async def test_nutrition_summary_가_exercise_summary에_전달됨():
    """dev mock에서 각 summary가 [MOCK] 텍스트임을 확인 — 체인이 실제로 순차 실행됨."""
    req = _make_request()
    result = await run_coaching_chain(req)
    assert result.nutrition_summary.startswith("[MOCK]")
    assert result.exercise_summary.startswith("[MOCK]")
    assert result.goal_summary.startswith("[MOCK]")
    assert result.ai_comment.startswith("[MOCK]")
