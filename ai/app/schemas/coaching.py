from pydantic import BaseModel
from typing import List, Optional, Literal


class DailyNutritionRecord(BaseModel):
    date: str                    # "2026-06-16"
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    calories_burned: float = 0.0


class RoutineSessionRecord(BaseModel):
    exercise_name: str
    successful_sets: int
    total_sets: int
    weight_kg: float
    session_date: str


class WeightRecord(BaseModel):
    date: str
    weight_kg: float


class WeeklyCoachingRequest(BaseModel):
    week_number: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    daily_nutrition: List[DailyNutritionRecord]
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    routine_sessions: List[RoutineSessionRecord]
    weight_records: List[WeightRecord]


class WeeklyCoachingResponse(BaseModel):
    ai_comment: str
    nutrition_summary: str
    exercise_summary: str
    goal_summary: str
    avg_calorie_rate: float
    achievement_days: int
    weight_trend: Optional[float]
