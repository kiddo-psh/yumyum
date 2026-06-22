from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.routers import ai_meal, ai_plan, ai_routine, food, ai_report, ai_coach, ai_chat
from app.services import rag_service
from app.config import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ENV=prod일 때만 ChromaDB·임베딩 모델 사전 로드 (cold-start 방지)
    if settings.env != "dev":
        rag_service._get_collection()
    yield


app = FastAPI(
    title="냠냠코치 AI Server",
    description="Spring Boot ↔ FastAPI AI 연동 서버",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(ai_meal.router)
app.include_router(ai_plan.router)
app.include_router(ai_routine.router)
app.include_router(food.router)
app.include_router(ai_report.router)
app.include_router(ai_coach.router)
app.include_router(ai_chat.router)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "nyamnyam-fastapi"}
