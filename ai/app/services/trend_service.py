import numpy as np
from datetime import date as _date
from typing import List, Optional


def calc_weight_trend(weights: List[float], dates: List[str]) -> Optional[float]:
    """
    선형 회귀로 주간 체중 변화량(kg/week) 계산.

    Args:
        weights: 날짜 오름차순으로 정렬된 체중 목록 (최소 2개 필요)
        dates: weights와 1:1 대응하는 ISO 날짜 문자열 목록 ("YYYY-MM-DD")
    Returns:
        kg/week 변화량 (양수=증가, 음수=감소), 데이터 2개 미만이면 None
    """
    if len(weights) < 2:
        return None
    d0 = _date.fromisoformat(dates[0])
    x = np.array([(_date.fromisoformat(d) - d0).days for d in dates], dtype=float)
    slope, _ = np.polyfit(x, weights, 1)
    return round(float(slope) * 7, 3)


_ADJUSTMENT_STEP = 100.0
_MIN_KCAL_FEMALE = 1200.0
_MIN_KCAL_MALE   = 1500.0
_VALID_GOALS = {"DIET", "MUSCLE", "HEALTH", "DISEASE"}


def calc_calorie_adjustment(
    current_kcal: float,
    health_goal: str,
    weight_trend: Optional[float],
    sex: str = "FEMALE",
) -> tuple[float, str]:
    """
    체중 추세와 건강 목표를 기반으로 목표 칼로리 조정량 계산.

    Args:
        current_kcal: 현재 목표 칼로리
        health_goal: "DIET" | "MUSCLE" | "HEALTH" | "DISEASE"
        weight_trend: kg/week 변화량. None이면 데이터 부족으로 유지
        sex: "MALE" | "FEMALE". 칼로리 하한선 기준 (남성 1500, 여성 1200). 기본값 FEMALE (안전한 하한)
    Returns:
        (new_kcal, reason)
    """
    if health_goal not in _VALID_GOALS:
        raise ValueError(f"지원하지 않는 health_goal: {health_goal!r}")

    if weight_trend is None:
        return current_kcal, "체중 데이터 부족으로 유지"

    adjustment = 0.0
    reason = ""

    if health_goal == "DIET":
        if weight_trend < -1.0:
            adjustment = +_ADJUSTMENT_STEP
            reason = f"체중 감소 속도 과다({weight_trend:+.2f}kg/주) → 칼로리 증량"
        elif weight_trend > -0.25:
            adjustment = -_ADJUSTMENT_STEP
            reason = f"감소 속도 부족({weight_trend:+.2f}kg/주) → 칼로리 감량"
        else:
            reason = f"적정 감소 속도({weight_trend:+.2f}kg/주) → 유지"
    elif health_goal == "MUSCLE":
        if weight_trend > 0.5:
            adjustment = -_ADJUSTMENT_STEP
            reason = f"체중 증가 속도 과다({weight_trend:+.2f}kg/주) → 칼로리 감량"
        elif weight_trend < 0.1:
            adjustment = +_ADJUSTMENT_STEP
            reason = f"증가 속도 부족({weight_trend:+.2f}kg/주) → 칼로리 증량"
        else:
            reason = f"적정 증가 속도({weight_trend:+.2f}kg/주) → 유지"
    else:  # HEALTH, DISEASE
        if abs(weight_trend) > 0.5:
            adjustment = -_ADJUSTMENT_STEP if weight_trend > 0 else +_ADJUSTMENT_STEP
            reason = f"체중 변동 과다({weight_trend:+.2f}kg/주) → 칼로리 조정"
        else:
            reason = f"체중 안정({weight_trend:+.2f}kg/주) → 유지"

    min_kcal = _MIN_KCAL_MALE if sex == "MALE" else _MIN_KCAL_FEMALE
    new_kcal = max(min_kcal, current_kcal + adjustment)
    return round(new_kcal, 1), reason
