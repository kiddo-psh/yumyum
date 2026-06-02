from fastapi import APIRouter
from app.schemas.plan import PlanGenerateRequest, PlanGenerateResponse
from app.services.claude_service import call_claude

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

# 체중 1kg당 단백질 권장량
PROTEIN_PER_KG = {
    "WEIGHT_LOSS": 2.0,   # 칼로리 적자 시 근손실 방지를 위해 높게 설정
    "MUSCLE_GAIN": 2.2,   # 최대 근합성 (ISSN 권장 상한)
    "MAINTAIN":    1.6,
}

# 잔여 칼로리를 탄수화물:지방으로 배분하는 비율 (탄수%, 지방%)
CARB_FAT_SPLIT = {
    "MUSCLE_GAIN": (55, 20),  # 운동 에너지·글리코겐 보충을 위해 탄수 비중 높게
    "WEIGHT_LOSS": (40, 35),  # 인슐린 제어·포만감을 위해 탄수 낮게
    "MAINTAIN":    (50, 25),
}


@router.post("/generate", response_model=PlanGenerateResponse)
async def generate_plan(req: PlanGenerateRequest):
    """
    F206 - 온보딩 AI 플랜 생성
    BMR(Mifflin-St Jeor) → TDEE → 목표별 칼로리·영양소 비율 계산
    """
    # ① BMR 계산 (Mifflin-St Jeor)
    if req.gender == "M":
        bmr = 10 * req.weight_kg + 6.25 * req.height_cm - 5 * req.age + 5
    else:
        bmr = 10 * req.weight_kg + 6.25 * req.height_cm - 5 * req.age - 161

    # ② TDEE
    multiplier = ACTIVITY_MULTIPLIER.get(req.activity_level, 1.55)
    tdee = bmr * multiplier

    # ③ 목표별 목표 칼로리
    adjust = GOAL_KCAL_ADJUST.get(req.health_goal, 0)
    target_kcal = tdee + adjust

    # 안전 하한선 (여성 1200 / 남성 1500)
    min_kcal = 1200 if req.gender == "F" else 1500
    target_kcal = max(target_kcal, min_kcal)

    # ④ 영양소 계산 (단백질: 체중 기반 / 탄수·지방: 목표별 비율로 잔여 칼로리 배분)
    target_protein_g = req.weight_kg * PROTEIN_PER_KG.get(req.health_goal, 1.6)
    remaining_kcal   = target_kcal - target_protein_g * 4
    carb_pct, fat_pct = CARB_FAT_SPLIT.get(req.health_goal, (50, 25))
    target_carb_g    = (remaining_kcal * (carb_pct / (carb_pct + fat_pct))) / 4
    target_fat_g     = (remaining_kcal * (fat_pct  / (carb_pct + fat_pct))) / 9

    # ⑤ Claude 코멘트
    prompt = f"""
사용자 정보: 성별={req.gender}, 나이={req.age}세, 키={req.height_cm}cm, 몸무게={req.weight_kg}kg,
활동량={req.activity_level}, 목표={req.health_goal}
목표 칼로리: {target_kcal:.0f}kcal (단백질 {target_protein_g:.0f}g / 탄수 {target_carb_g:.0f}g / 지방 {target_fat_g:.0f}g)

이 사용자에게 맞는 식단 시작 조언을 2~3문장으로 친근하게 작성해주세요.
"""
    ai_comment = await call_claude(prompt)

    return PlanGenerateResponse(
        bmr=round(bmr, 1),
        tdee=round(tdee, 1),
        target_kcal=round(target_kcal, 1),
        target_protein_g=round(target_protein_g, 1),
        target_carb_g=round(target_carb_g, 1),
        target_fat_g=round(target_fat_g, 1),
        ai_comment=ai_comment,
    )
