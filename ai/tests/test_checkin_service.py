import pytest
from app.services.checkin_service import classify_checkin, calc_adjustment_options


def test_달성률_30미만이면_VERY_LOW():
    assert classify_checkin(20.0) == "VERY_LOW"
    assert classify_checkin(0.0)  == "VERY_LOW"
    assert classify_checkin(29.9) == "VERY_LOW"


def test_달성률_30이상이면_LOW():
    assert classify_checkin(30.0) == "LOW"
    assert classify_checkin(49.9) == "LOW"


def test_DIET_옵션_3개_반환():
    options = calc_adjustment_options("DIET", 1800.0, "FEMALE")
    assert len(options) == 3


def test_MUSCLE_옵션_3개_반환():
    options = calc_adjustment_options("MUSCLE", 2200.0, "MALE")
    assert len(options) == 3


def test_HEALTH_옵션_2개_반환():
    options = calc_adjustment_options("HEALTH", 2000.0, "FEMALE")
    assert len(options) == 2


def test_DISEASE_옵션_2개_반환():
    options = calc_adjustment_options("DISEASE", 2000.0, "FEMALE")
    assert len(options) == 2


def test_KEEP_옵션이_항상_첫번째():
    for goal in ["DIET", "MUSCLE", "HEALTH", "DISEASE"]:
        options = calc_adjustment_options(goal, 2000.0, "FEMALE")
        assert options[0].option_id == "KEEP"


def test_KEEP_옵션_new_target_kcal_현재_칼로리_유지():
    for goal in ["DIET", "MUSCLE", "HEALTH", "DISEASE"]:
        options = calc_adjustment_options(goal, 2000.0, "FEMALE")
        assert options[0].new_target_kcal == 2000.0


def test_달성률_50이상이면_ValueError():
    with pytest.raises(ValueError):
        classify_checkin(50.0)
    with pytest.raises(ValueError):
        classify_checkin(100.0)


def test_DIET_RELAX_옵션은_칼로리_100_증량():
    options = calc_adjustment_options("DIET", 1800.0, "FEMALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal == 1900.0


def test_MUSCLE_RELAX_옵션은_칼로리_100_감량():
    options = calc_adjustment_options("MUSCLE", 2200.0, "FEMALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal == 2100.0


def test_MUSCLE_RELAX_여성_하한선_1200_보장():
    options = calc_adjustment_options("MUSCLE", 1250.0, "FEMALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal >= 1200.0


def test_MUSCLE_RELAX_남성_하한선_1500_보장():
    options = calc_adjustment_options("MUSCLE", 1550.0, "MALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal >= 1500.0


def test_CHANGE_GOAL_옵션은_new_target_kcal이_None():
    options = calc_adjustment_options("DIET", 1800.0, "FEMALE")
    change = next(o for o in options if o.option_id == "CHANGE_GOAL")
    assert change.new_target_kcal is None


def test_유효하지_않은_health_goal_ValueError():
    with pytest.raises(ValueError):
        calc_adjustment_options("UNKNOWN", 2000.0, "FEMALE")
