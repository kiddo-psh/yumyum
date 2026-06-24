from app.schemas.chat import MealContext

_HEALTH_GOAL_KR = {
    "DIET":    "다이어트",
    "MUSCLE":  "근육 증가",
    "HEALTH":  "건강 유지",
    "DISEASE": "질환 관리",
}


def build_prompt(message: str, docs: list[dict], context: MealContext | None) -> str:
    """RAG 검색 결과와 개인 식단 컨텍스트를 합쳐 Claude 프롬프트를 조립한다."""
    lines = [
        "당신은 영양 전문 AI 코치입니다. "
        "아래 정보를 바탕으로 친절하고 구체적으로 답변하세요.\n"
    ]

    if docs:
        lines.append("[참고 식품 정보]")
        for d in docs:
            lines.append(f"- {d['name']}: {d['info']}")
        lines.append("")

    if context:
        goal_kr = _HEALTH_GOAL_KR.get(context.health_goal, context.health_goal)
        lines.append("[사용자 오늘 식단]")
        lines.append(f"- 단백질: {context.total_protein_g}g / 목표 {context.target_protein_g}g")
        lines.append(f"- 탄수화물: {context.total_carb_g}g / 목표 {context.target_carb_g}g")
        lines.append(f"- 지방: {context.total_fat_g}g / 목표 {context.target_fat_g}g")
        lines.append(f"- 칼로리: {context.total_kcal}kcal / 목표 {context.target_kcal}kcal")
        lines.append(f"- 건강 목표: {goal_kr}")
        lines.append("")

    lines.append(f"[질문]\n{message}")
    return "\n".join(lines)
