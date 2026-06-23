from pydantic import BaseModel
from typing import List


# ── generate 스키마 ──────────────────────────────────────────────────

class RoutineExerciseSchema(BaseModel):
    name: str
    sets: int
    reps: int
    weight_kg: float


class RoutineDaySchema(BaseModel):
    day_label: str
    exercises: List[RoutineExerciseSchema]


class RoutineGenerateRequest(BaseModel):
    gender: str
    age: int
    weight_kg: float
    height_cm: float
    health_goal: str
    has_existing_routine: bool = False
    days_per_week: int
    split_type: str
    split_labels: List[str]


class RoutineGenerateResponse(BaseModel):
    routine_name: str
    days: List[RoutineDaySchema]
    ai_comment: str


# ── adjust 스키마 ────────────────────────────────────────────────────

class ExerciseInfo(BaseModel):
    exercise_id: int
    day_label: str
    exercise_name: str
    target_sets: int
    target_reps: int
    target_weight_kg: float
    order_index: int


class SessionSetData(BaseModel):
    exercise_id: int
    exercise_name: str
    target_sets: int
    actual_sets_completed: int
    avg_actual_reps: float
    avg_actual_weight_kg: float


class RecentSessionData(BaseModel):
    session_date: str
    sets: List[SessionSetData]


class RoutineAdjustRequest(BaseModel):
    routine_id: int
    current_week_number: int
    exercises: List[ExerciseInfo]
    recent_sessions: List[RecentSessionData]


class ExerciseAdjustment(BaseModel):
    exercise_id: int
    action: str  # UP | HOLD | DOWN | VOLUME_UP | DELOAD
    new_weight_kg: float
    new_sets: int
    new_reps: int
    reason: str


class RoutineAdjustResponse(BaseModel):
    adjustments: List[ExerciseAdjustment]
    ai_comment: str
    next_week_number: int
