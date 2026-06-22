from pydantic import BaseModel
from typing import Optional, List


class MealContext(BaseModel):
    total_protein_g: float
    target_protein_g: float
    total_carb_g: float
    target_carb_g: float
    total_fat_g: float
    target_fat_g: float
    total_kcal: float
    target_kcal: float
    health_goal: str  # DIET | MUSCLE | HEALTH | DISEASE


class ChatRequest(BaseModel):
    message: str
    context: Optional[MealContext] = None


class ChatSource(BaseModel):
    name: str
    info: str


class ChatResponse(BaseModel):
    answer: str
    sources: List[ChatSource]
