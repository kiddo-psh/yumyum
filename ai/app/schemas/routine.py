from pydantic import BaseModel
from typing import List


class RoutineExerciseSchema(BaseModel):
    name: str
    sets: int
    reps: int
    weight_kg: float


class RoutineDaySchema(BaseModel):
    day_label: str
    exercises: List[RoutineExerciseSchema]


class RoutineGenerateRequest(BaseModel):
    gender: str           # "M" | "F"
    age: int
    weight_kg: float
    height_cm: float
    health_goal: str      # "MUSCLE_GAIN" | "WEIGHT_LOSS" | "MAINTAIN"
    days_per_week: int
    split_type: str       # e.g. "UPPER_LOWER_4"
    split_labels: List[str]  # e.g. ["상체", "하체", "상체", "하체"]


class RoutineGenerateResponse(BaseModel):
    routine_name: str
    days: List[RoutineDaySchema]
    ai_comment: str
