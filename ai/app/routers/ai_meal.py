import json

from fastapi import APIRouter, HTTPException
from app.schemas.meal import LastRecommendRequest, LastRecommendResponse, MealRecommendation
from app.services.claude_service import call_claude

router = APIRouter(prefix="/ai/meal", tags=["AI Meal"])


@router.post("/last-recommend", response_model=LastRecommendResponse)
async def last_recommend(req: LastRecommendRequest):
    """
    F802 - 마지막 끼니 추천
    트리거 조건:
      - 잔여 칼로리가 1끼 분량(목표 ÷ 끼니 수) ± 20% 범위일 때
      - 오후 5시 이후 (Spring에서 시간 체크 후 호출)
      - 오늘 최소 1끼 이상 기록됨
    """
    if req.meal_count < 1:
        raise HTTPException(status_code=400, detail="오늘 기록된 끼니가 없어 추천을 생성할 수 없습니다.")

    # ① 잔여 영양소 계산
    remain_kcal    = req.target_kcal      - req.total_kcal
    remain_protein = req.target_protein_g - req.total_protein_g
    remain_carb    = req.target_carb_g    - req.total_carb_g
    remain_fat     = req.target_fat_g     - req.total_fat_g

    # ② 부족 영양소 우선순위 (달성률이 가장 낮은 영양소)
    rates = {
        "protein": req.total_protein_g / req.target_protein_g if req.target_protein_g > 0 else 1,
        "carb":    req.total_carb_g    / req.target_carb_g    if req.target_carb_g    > 0 else 1,
        "fat":     req.total_fat_g     / req.target_fat_g     if req.target_fat_g     > 0 else 1,
    }
    priority = min(rates, key=rates.get)

    # ③ Claude 프롬프트 생성
    prompt = f"""
당신은 영양사 AI입니다. 아래 잔여 영양소를 채울 수 있는 저녁 식단 3가지를 추천해주세요.

[잔여 영양소]
- 칼로리: {remain_kcal:.0f} kcal
- 단백질: {remain_protein:.1f} g
- 탄수화물: {remain_carb:.1f} g
- 지방: {remain_fat:.1f} g
- 가장 부족한 영양소: {priority}

[조건]
- 과도하게 많은 양이 아닌 현실적인 한 끼 분량으로 제안
- 잔여 영양소를 대부분 채울 수 있는 식단 위주로 추천
- 각 식단은 음식명, 칼로리(kcal), 단백질(g), 탄수화물(g), 지방(g), 추천 이유를 포함

JSON 배열 형식으로만 응답하세요:
[
  {{"name": "음식명", "kcal": 숫자, "protein_g": 숫자, "carb_g": 숫자, "fat_g": 숫자, "reason": "이유"}},
  ...
]
"""

    _FALLBACK = [
        MealRecommendation(name="닭가슴살 샐러드",    kcal=380, protein_g=42, carb_g=18, fat_g=12, reason="단백질 보충에 최적"),
        MealRecommendation(name="두부된장찌개+현미밥", kcal=420, protein_g=22, carb_g=58, fat_g=9,  reason="균형잡힌 한식"),
        MealRecommendation(name="연어구이+고구마",     kcal=390, protein_g=35, carb_g=32, fat_g=14, reason="오메가3 + 복합 탄수화물"),
    ]

    try:
        raw = await call_claude(prompt)

        # ```json 블록 또는 순수 배열 모두 처리
        cleaned = raw.strip()
        if "```" in cleaned:
            parts = cleaned.split("```")
            cleaned = parts[1] if len(parts) > 1 else cleaned
            if cleaned.startswith("json"):
                cleaned = cleaned[4:]

        meals = json.loads(cleaned)
        recommendations = [MealRecommendation(**m) for m in meals]
    except Exception as e:
        # GMS API 장애·파싱 실패 시 fallback (서버 500 방지)
        print(f"[ai_meal] fallback 사용 — 원인: {type(e).__name__}: {e}")
        recommendations = _FALLBACK

    return LastRecommendResponse(
        recommendations=recommendations,
        priority_nutrient=priority,
        ai_comment=f"오늘 {priority} 섭취가 가장 부족합니다. 아래 식단으로 하루를 마무리해보세요!",
    )
