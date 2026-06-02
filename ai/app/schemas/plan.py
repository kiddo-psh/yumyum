from pydantic import BaseModel


class PlanGenerateRequest(BaseModel):
    gender: str            # "M" | "F"
    age: int
    height_cm: float
    weight_kg: float
    activity_level: str    # "sedentary" | "light" | "moderate" | "active" | "very_active"
    health_goal: str       # "WEIGHT_LOSS" | "MUSCLE_GAIN" | "MAINTAIN"


class PlanGenerateResponse(BaseModel):
    bmr: float
    tdee: float
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    ai_comment: str
