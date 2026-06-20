from app.schemas.report import AdjustmentOption

_MIN_KCAL: dict[str, float] = {"MALE": 1500.0, "FEMALE": 1200.0}
_RELAX_STEP = 100.0
_VALID_GOALS = {"DIET", "MUSCLE", "HEALTH", "DISEASE"}


def classify_checkin(achievement_rate: float) -> str:
    """
    2주 달성률 → 체크인 타입.
    < 30% → VERY_LOW, 30~50% → LOW
    Spring이 50% 미만일 때만 호출하지만 방어적으로 검증한다.
    """
    if achievement_rate >= 50.0:
        raise ValueError(f"체크인은 달성률 50% 미만일 때만 호출됩니다: {achievement_rate}")
    if achievement_rate < 30.0:
        return "VERY_LOW"
    return "LOW"


def calc_adjustment_options(
    health_goal: str,
    current_target_kcal: float,
    sex: str,
) -> list[AdjustmentOption]:
    """
    건강 목표별 조정 옵션 반환.
    DIET/MUSCLE → [KEEP, RELAX, CHANGE_GOAL] (3개)
    HEALTH/DISEASE → [KEEP, CHANGE_GOAL] (2개)
    """
    if health_goal not in _VALID_GOALS:
        raise ValueError(f"지원하지 않는 health_goal: {health_goal!r}")

    min_kcal = _MIN_KCAL.get(sex, 1200.0)

    keep = AdjustmentOption(
        option_id="KEEP",
        label="목표 유지",
        description="현재 목표를 그대로 이어나갑니다. 다음 2주를 더욱 집중해서 도전해보세요!",
        new_target_kcal=current_target_kcal,
    )
    change_goal = AdjustmentOption(
        option_id="CHANGE_GOAL",
        label="목표 변경 검토",
        description="건강 목표 타입 변경을 고려해보세요. 지속 가능한 목표가 장기적으로 더 효과적입니다.",
        new_target_kcal=None,
    )

    if health_goal == "DIET":
        relaxed_kcal = round(current_target_kcal + _RELAX_STEP, 1)
        relax = AdjustmentOption(
            option_id="RELAX",
            label="목표 완화",
            description=f"칼로리 목표를 {relaxed_kcal:.0f}kcal로 높여 지속 가능한 다이어트를 이어가세요.",
            new_target_kcal=relaxed_kcal,
        )
        return [keep, relax, change_goal]

    if health_goal == "MUSCLE":
        relaxed_kcal = round(max(min_kcal, current_target_kcal - _RELAX_STEP), 1)
        relax = AdjustmentOption(
            option_id="RELAX",
            label="목표 완화",
            description=f"칼로리 목표를 {relaxed_kcal:.0f}kcal로 낮춰 부담 없이 꾸준히 진행해보세요.",
            new_target_kcal=relaxed_kcal,
        )
        return [keep, relax, change_goal]

    # HEALTH, DISEASE
    return [keep, change_goal]
