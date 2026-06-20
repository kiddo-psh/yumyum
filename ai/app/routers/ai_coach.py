from fastapi import APIRouter, HTTPException
from app.schemas.coach import ExerciseCoachRequest, ExerciseCoachResponse
from app.services.claude_service import call_claude
from app.services.coach_service import pick_focus_exercise

router = APIRouter(tags=["AI Coach"])


@router.post("/ai/exercise/coach", response_model=ExerciseCoachResponse)
async def exercise_coach(req: ExerciseCoachRequest):
    """
    F702 - 루틴 기반 맞춤 운동 코칭
    현재 루틴 성과를 분석해 집중 운동과 개인화 코칭 코멘트를 반환한다.
    """
    try:
        focus = pick_focus_exercise(req.exercises)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

    exercise_summary = "\n".join(
        f"- {e.exercise_name}: {e.current_sets}세트×{e.current_reps}회 "
        f"{e.current_weight_kg}kg, 성공률 {e.success_rate:.0%}, 상태={e.last_action}"
        for e in req.exercises
    )

    prompt = (
        f"[{req.week_number}주차 운동 코칭] 건강 목표: {req.health_goal}\n"
        f"현재 루틴:\n{exercise_summary}\n"
        f"집중 운동: {focus}\n"
        f"위 데이터를 바탕으로 {focus} 위주의 자세 교정·동기부여·다음 주 전략을 "
        "3~4문장으로 한국어로 코칭해주세요."
    )

    overall_comment = await call_claude(prompt, max_tokens=400)

    return ExerciseCoachResponse(
        focus_exercise=focus,
        overall_comment=overall_comment,
    )
