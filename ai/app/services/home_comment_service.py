from app.schemas.home import HomeCommentRequest
from app.services.claude_service import call_claude
from app.config import settings

_MOCK_COMMENTS = {
    "DIET": "다이어트 목표를 향해 오늘도 꾸준히 나아가고 있어요. 화이팅!",
    "MUSCLE": "근육 증가 목표, 오늘도 착실히 쌓아가고 있어요!",
    "HEALTH": "건강한 습관이 쌓이고 있어요. 오늘도 좋은 하루 보내세요!",
    "DISEASE": "건강 관리를 꾸준히 이어가고 있어요. 오늘도 잘 하고 있어요!",
}

_GOAL_LABELS = {
    "DIET": "다이어트",
    "MUSCLE": "근육 증가",
    "HEALTH": "건강 유지",
    "DISEASE": "질환 관리",
}


async def generate_home_comment(req: HomeCommentRequest) -> str:
    if settings.env == "dev":
        return _MOCK_COMMENTS.get(req.health_goal, "오늘도 건강한 하루 보내세요!")

    goal_label = _GOAL_LABELS.get(req.health_goal, req.health_goal)
    kcal_percent = round(req.kcal_rate * 100)
    streak_msg = f"Streak {req.current_streak}일 연속 달성 중" if req.current_streak > 0 else "오늘부터 Streak 시작"

    prompt = (
        f"당신은 건강 코치입니다. 아래 사용자 현황을 바탕으로 한 줄(50자 이내) 동기부여 메시지를 작성하세요.\n\n"
        f"[사용자 현황]\n"
        f"- 건강 목표: {goal_label}\n"
        f"- 오늘 칼로리 달성률: {kcal_percent}%\n"
        f"- {streak_msg}\n\n"
        f"[규칙]\n"
        f"- 목표({goal_label})를 중심으로 작성\n"
        f"- 특정 음식이나 영양소 수치 언급 금지\n"
        f"- 구어체, 긍정적 톤, 한국어, 50자 이내\n"
        f"- 한 문장만 출력"
    )

    try:
        return await call_claude(prompt, max_tokens=100)
    except Exception:
        return _MOCK_COMMENTS.get(req.health_goal, "오늘도 건강한 하루 보내세요!")
