from pydantic import BaseModel
from typing import List


class LastRecommendRequest(BaseModel):
    total_kcal: float
    total_protein_g: float
    total_carb_g: float
    total_fat_g: float
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    meal_count: int  # 오늘 기록된 끼니 수 (최소 1 이상이어야 추천 발동)


class MealRecommendation(BaseModel):
    name: str
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    reason: str


class LastRecommendResponse(BaseModel):
    recommendations: List[MealRecommendation]
    priority_nutrient: str  # "protein" | "carb" | "fat"
    ai_comment: str


class NutrientRate(BaseModel):
    calorie_rate: float   # 달성률 % (actual/target * 100)
    protein_rate: float
    carb_rate: float
    fat_rate: float


class DietAnalyzeRequest(BaseModel):
    total_kcal: float
    total_protein_g: float
    total_carb_g: float
    total_fat_g: float
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    health_goal: str   # DIET | MUSCLE | HEALTH | DISEASE
    meal_date: str     # "2026-06-15" (로깅·코멘트 생성용)


class DietAnalyzeResponse(BaseModel):
    calorie_rate: float          # 칼로리 달성률 %
    protein_rate: float          # 단백질 달성률 %
    carb_rate: float             # 탄수화물 달성률 %
    fat_rate: float              # 지방 달성률 %
    balance_score: float         # 균형 점수 0~100 (100에 가까울수록 균형)
    weak_nutrients: List[str]    # 달성률 80% 미만 영양소 목록
    excess_nutrients: List[str]  # 달성률 120% 초과 영양소 목록
    ai_comment: str
