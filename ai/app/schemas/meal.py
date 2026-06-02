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
