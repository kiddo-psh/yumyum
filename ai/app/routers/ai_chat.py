from fastapi import APIRouter
from app.schemas.chat import ChatRequest, ChatResponse, ChatSource
from app.services.claude_service import call_claude
from app.services import rag_service, chat_service
from app.config import settings

router = APIRouter(tags=["AI Chat"])


@router.post("/ai/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    """F703 - RAG 기반 영양 채팅. 식품안전처 DB 검색 + 개인 식단 컨텍스트 주입."""
    if settings.env == "dev":
        return _mock_response(req.message)

    try:
        docs = rag_service.search(req.message)
    except Exception:
        docs = []

    prompt = chat_service.build_prompt(req.message, docs, req.context)
    answer = await call_claude(prompt, max_tokens=500)
    sources = [ChatSource(name=d["name"], info=d["info"]) for d in docs]
    return ChatResponse(answer=answer, sources=sources)


def _mock_response(message: str) -> ChatResponse:
    return ChatResponse(
        answer="[MOCK] 닭가슴살과 두부를 추가하면 단백질 목표 달성에 도움이 됩니다.",
        sources=[
            ChatSource(name="닭가슴살", info="100g당 단백질 23g, 열량 109kcal"),
            ChatSource(name="두부",     info="100g당 단백질 8g, 열량 76kcal"),
        ],
    )
