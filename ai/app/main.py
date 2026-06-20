from fastapi import FastAPI
from app.routers import ai_meal, ai_plan, ai_routine, food, ai_report

app = FastAPI(
    title="냠냠코치 AI Server",
    description="Spring Boot ↔ FastAPI AI 연동 서버",
    version="1.0.0",
)

app.include_router(ai_meal.router)
app.include_router(ai_plan.router)
app.include_router(ai_routine.router)
app.include_router(food.router)
app.include_router(ai_report.router)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "nyamnyam-fastapi"}
