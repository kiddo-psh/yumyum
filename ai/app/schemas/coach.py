# ai/app/schemas/coach.py
from pydantic import BaseModel
from typing import List, Literal


class ExerciseCoachItem(BaseModel):
    exercise_name: str
    current_weight_kg: float
    current_sets: int
    current_reps: int
    last_action: str        # UP | HOLD | DOWN | VOLUME_UP | DELOAD
    success_rate: float     # 0.0~1.0 (마지막 세션 세트 성공률)


class ExerciseCoachRequest(BaseModel):
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    week_number: int
    exercises: List[ExerciseCoachItem]


class ExerciseCoachResponse(BaseModel):
    focus_exercise: str     # 가장 개선이 필요한 운동명
    overall_comment: str    # AI 코칭 코멘트 (3~4문장)
