from fastapi import APIRouter
from app.schemas.plan import PlanGenerateRequest, PlanGenerateResponse

router = APIRouter(prefix="/ai/plan", tags=["AI Plan"])

ACTIVITY_MULTIPLIER = {
    "sedentary":   1.2,
    "light":       1.375,
    "moderate":    1.55,
    "active":      1.725,
    "very_active": 1.9,
}

GOAL_KCAL_ADJUST = {
    "WEIGHT_LOSS":  -500,
    "MUSCLE_GAIN":  +300,
    "MAINTAIN":       0,
}

PROTEIN_PER_KG = {
    "WEIGHT_LOSS": 2.0,
    "MUSCLE_GAIN": 2.2,
    "MAINTAIN":    1.6,
}

CARB_FAT_SPLIT = {
    "MUSCLE_GAIN": (55, 20),
    "WEIGHT_LOSS": (40, 35),
    "MAINTAIN":    (50, 25),
}

# 플랜 환영 코멘트 — 목표별 템플릿
# 초기 플랜 생성 시점에는 쌓인 데이터가 없어 Claude 코멘트가 의미 없고 대기만 길어짐.
# 개인화 AI 코멘트는 데이터가 쌓인 홈화면·주간 리포트에서 제공.
_PLAN_COMMENTS = {
    "WEIGHT_LOSS": (
        "하루 {kcal:.0f}kcal 목표로 시작해요! "
        "단백질 {protein:.0f}g을 채우면 근손실 없이 체중을 줄일 수 있어요. "
        "식단 기록부터 차근차근 시작해봐요."
    ),
    "MUSCLE_GAIN": (
        "근육 증가를 위해 하루 {kcal:.0f}kcal, 단백질 {protein:.0f}g을 목표로 잡았어요! "
        "탄수화물도 충분히 섭취해야 운동 에너지가 나와요. "
        "오늘 식단부터 기록해볼까요?"
    ),
    "MAINTAIN": (
        "건강 유지를 위한 하루 {kcal:.0f}kcal 플랜이 준비됐어요! "
        "균형 잡힌 식단이 핵심이에요. "
        "매일 꾸준히 기록하면 냠냠코치가 더 정확한 조언을 드릴게요."
    ),
}


@router.post("/generate", response_model=PlanGenerateResponse)
async def generate_plan(req: PlanGenerateRequest):
    """
    F206 - 온보딩 AI 플랜 생성
    BMR(Mifflin-St Jeor) → TDEE → 목표별 칼로리·영양소 계산 (순수 수식, Claude 미사용)
    개인화 코멘트는 데이터 축적 후 홈화면·주간 리포트에서 제공
    """
    # ① BMR (Mifflin-St Jeor)
    if req.gender == "M":
        bmr = 10 * req.weight_kg + 6.25 * req.height_cm - 5 * req.age + 5
    else:
        bmr = 10 * req.weight_kg + 6.25 * req.height_cm - 5 * req.age - 161

    # ② TDEE
    multiplier = ACTIVITY_MULTIPLIER.get(req.activity_level, 1.55)
    tdee = bmr * multiplier

    # ③ 목표 칼로리 (하한선: 여성 1200 / 남성 1500)
    target_kcal = max(tdee + GOAL_KCAL_ADJUST.get(req.health_goal, 0),
                      1200 if req.gender == "F" else 1500)

    # ④ 영양소
    target_protein_g = req.weight_kg * PROTEIN_PER_KG.get(req.health_goal, 1.6)
    remaining_kcal   = target_kcal - target_protein_g * 4
    carb_pct, fat_pct = CARB_FAT_SPLIT.get(req.health_goal, (50, 25))
    target_carb_g    = (remaining_kcal * (carb_pct / (carb_pct + fat_pct))) / 4
    target_fat_g     = (remaining_kcal * (fat_pct  / (carb_pct + fat_pct))) / 9

    # ⑤ 목표별 템플릿 코멘트 (즉시 반환)
    template = _PLAN_COMMENTS.get(req.health_goal, _PLAN_COMMENTS["MAINTAIN"])
    ai_comment = template.format(kcal=target_kcal, protein=target_protein_g)

    return PlanGenerateResponse(
        bmr=round(bmr, 1),
        tdee=round(tdee, 1),
        target_kcal=round(target_kcal, 1),
        target_protein_g=round(target_protein_g, 1),
        target_carb_g=round(target_carb_g, 1),
        target_fat_g=round(target_fat_g, 1),
        ai_comment=ai_comment,
    )
