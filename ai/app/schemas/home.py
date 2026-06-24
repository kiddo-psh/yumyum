from pydantic import BaseModel
from typing import Literal


class HomeCommentRequest(BaseModel):
    member_id: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    current_streak: int
    kcal_rate: float        # 0.0 ~ 1.0+ (달성률)
    remaining_kcal: float
    protein_g: float
    carb_g: float
    fat_g: float


class HomeCommentResponse(BaseModel):
    comment: str
