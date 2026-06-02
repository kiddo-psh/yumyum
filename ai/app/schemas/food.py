from pydantic import BaseModel
from typing import List, Optional


class FoodItem(BaseModel):
    food_cd: str
    name: str
    serving_size_g: float
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    sugar_g: Optional[float] = None
    fiber_g: Optional[float] = None
    source: str  # "MFDS" | "MANUAL" | "AI"


class FoodSearchResponse(BaseModel):
    items: List[FoodItem]
    total: int
    page: int
    size: int
    query: str
