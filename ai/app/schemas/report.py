from pydantic import BaseModel
from typing import List, Optional, Literal


class DailyNutrition(BaseModel):
    date: str           # "2026-06-13"
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    calories_burned: float = 0.0   # 운동 소모 칼로리


class WeightRecord(BaseModel):
    date: str           # "2026-06-13"
    weight_kg: float


class WeeklyReportRequest(BaseModel):
    program_id: int
    week_number: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    daily_nutrition: List[DailyNutrition]
    weight_records: List[WeightRecord]


class WeeklyReportResponse(BaseModel):
    avg_calorie_rate: float
    avg_protein_rate: float
    avg_carb_rate: float
    avg_fat_rate: float
    achievement_days: int
    weight_trend: Optional[float]
    ai_comment: str


class WeeklyAdjustRequest(BaseModel):
    program_id: int
    week_number: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    sex: Literal["MALE", "FEMALE"]
    current_target_kcal: float
    weight_trend: Optional[float]


class WeeklyAdjustResponse(BaseModel):
    new_target_kcal: float
    adjustment: float
    reason: str
    ai_comment: str
