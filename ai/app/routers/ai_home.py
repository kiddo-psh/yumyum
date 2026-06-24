from fastapi import APIRouter
from app.schemas.home import HomeCommentRequest, HomeCommentResponse
from app.services.home_comment_service import generate_home_comment

router = APIRouter(prefix="/ai/home", tags=["AI Home"])


@router.post("/comment", response_model=HomeCommentResponse)
async def home_comment(req: HomeCommentRequest):
    comment = await generate_home_comment(req)
    return HomeCommentResponse(comment=comment)
