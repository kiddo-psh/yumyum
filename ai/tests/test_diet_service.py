import pytest
from app.services.diet_service import calculate_diet_analysis


def test_목표치_정확히_달성하면_균형점수_100():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert rates["calorie_rate"] == 100.0
    assert rates["protein_rate"] == 100.0
    assert score == 100.0
    assert weak == []
    assert excess == []


def test_단백질_부족하면_weak_nutrients에_포함():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=100, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert "protein" in weak
    assert rates["protein_rate"] == pytest.approx(66.67, abs=0.1)


def test_지방_과다하면_excess_nutrients에_포함():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=80,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert "fat" in excess
    assert rates["fat_rate"] == pytest.approx(133.33, abs=0.1)


def test_target이_0이면_달성률_100으로_처리():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=0,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=0,
    )
    assert rates["fat_rate"] == 100.0
    assert "fat" not in weak
    assert "fat" not in excess


def test_균형점수는_편차가_클수록_낮아진다():
    _, score_balanced, _, _ = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    _, score_unbalanced, _, _ = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=75, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert score_balanced > score_unbalanced
