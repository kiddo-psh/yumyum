import pytest
from app.services.trend_service import calc_weight_trend, calc_calorie_adjustment


# ── calc_weight_trend ────────────────────────────────────────────────

def test_체중이_일정하면_추세_0():
    result = calc_weight_trend([70.0, 70.0, 70.0, 70.0])
    assert result == pytest.approx(0.0, abs=0.01)


def test_체중이_꾸준히_감소하면_음수_반환():
    # 하루 -1/7 kg씩 7일간 선형 감소 → 선형 회귀 기울기 = -1.0 kg/week
    weights = [71.0, 70.857, 70.714, 70.571, 70.429, 70.286, 70.143]
    result = calc_weight_trend(weights)
    assert result == pytest.approx(-1.0, abs=0.05)


def test_체중이_꾸준히_증가하면_양수_반환():
    weights = [70.0, 70.1, 70.2, 70.3, 70.4, 70.5, 70.6]
    result = calc_weight_trend(weights)
    assert result is not None
    assert result > 0


def test_데이터_1개면_None_반환():
    assert calc_weight_trend([70.0]) is None


def test_빈_리스트면_None_반환():
    assert calc_weight_trend([]) is None


# ── calc_calorie_adjustment ──────────────────────────────────────────

def test_DIET_감소_과다시_칼로리_증량():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", -1.5, sex="FEMALE")
    assert new_kcal == 1900.0
    assert "증량" in reason


def test_DIET_감소_적정시_유지():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", -0.5, sex="FEMALE")
    assert new_kcal == 1800.0
    assert "유지" in reason


def test_DIET_감소_부족시_칼로리_감량():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", 0.1, sex="FEMALE")
    assert new_kcal == 1700.0
    assert "감량" in reason


def test_MUSCLE_증가_과다시_칼로리_감량():
    new_kcal, reason = calc_calorie_adjustment(2200.0, "MUSCLE", 0.8, sex="FEMALE")
    assert new_kcal == 2100.0
    assert "감량" in reason


def test_MUSCLE_증가_적정시_유지():
    new_kcal, reason = calc_calorie_adjustment(2200.0, "MUSCLE", 0.3, sex="FEMALE")
    assert new_kcal == 2200.0
    assert "유지" in reason


def test_MUSCLE_증가_부족시_칼로리_증량():
    new_kcal, reason = calc_calorie_adjustment(2200.0, "MUSCLE", 0.0, sex="FEMALE")
    assert new_kcal == 2300.0
    assert "증량" in reason


def test_HEALTH_체중_안정시_유지():
    new_kcal, reason = calc_calorie_adjustment(2000.0, "HEALTH", 0.2, sex="FEMALE")
    assert new_kcal == 2000.0
    assert "유지" in reason


def test_HEALTH_체중_변동_과다시_조정():
    new_kcal, reason = calc_calorie_adjustment(2000.0, "HEALTH", 0.8, sex="FEMALE")
    assert new_kcal == 1900.0


def test_추세_None이면_현재_칼로리_유지():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", None, sex="FEMALE")
    assert new_kcal == 1800.0
    assert "부족" in reason


def test_칼로리_하한선_1200_미만으로_내려가지_않음():
    new_kcal, _ = calc_calorie_adjustment(1250.0, "DIET", 0.5, sex="FEMALE")
    assert new_kcal >= 1200.0


def test_HEALTH_체중_음수_변동_과다시_칼로리_증량():
    # 체중이 빠르게 감소하는 경우 칼로리 증량
    new_kcal, reason = calc_calorie_adjustment(2000.0, "HEALTH", -0.8, sex="FEMALE")
    assert new_kcal == 2100.0


def test_DISEASE_목표도_HEALTH와_동일_규칙_적용():
    # DISEASE는 HEALTH와 동일한 else 분기 처리
    new_kcal, reason = calc_calorie_adjustment(2000.0, "DISEASE", 0.2, sex="FEMALE")
    assert new_kcal == 2000.0
    assert "유지" in reason


def test_남성_하한선은_1500():
    new_kcal, _ = calc_calorie_adjustment(1550.0, "DIET", 0.5, sex="MALE")
    assert new_kcal >= 1500.0


def test_여성_하한선은_1200():
    new_kcal, _ = calc_calorie_adjustment(1250.0, "DIET", 0.5, sex="FEMALE")
    assert new_kcal >= 1200.0


def test_유효하지_않은_health_goal_ValueError_발생():
    with pytest.raises(ValueError):
        calc_calorie_adjustment(2000.0, "UNKNOWN", 0.0, sex="FEMALE")
