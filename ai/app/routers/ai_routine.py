import json
from fastapi import APIRouter, HTTPException
from app.schemas.routine import (
    RoutineGenerateRequest, RoutineGenerateResponse,
    RoutineAdjustRequest, RoutineAdjustResponse, ExerciseAdjustment,
)
from app.services.claude_service import call_claude
from app.services.routine_service import calculate_adjustment

router = APIRouter(prefix="/ai/routine", tags=["AI Routine"])

_REASON = {
    "UP":         "모든 세트·반복 성공 → 무게 증량",
    "HOLD":       "일부 세트 성공 → 무게 유지",
    "DOWN":       "2회 연속 실패 → 무게 감량",
    "VOLUME_UP":  "3회 연속 성공 → 볼륨 증가",
    "DELOAD":     "4주 누적 피로 → 디로드 적용",
}


@router.post("/generate", response_model=RoutineGenerateResponse)
async def generate_routine(req: RoutineGenerateRequest):
    """
    F901 - AI 루틴 생성
    사용자 정보 & 분할 유형을 기반으로 맞춤 운동 루틴 생성
    """
    prompt = f"""
사용자 정보: 성별={req.gender}, 나이={req.age}세, 키={req.height_cm}cm, 몸무게={req.weight_kg}kg, 목표={req.health_goal}
운동 분할: 주 {req.days_per_week}회 [{', '.join(req.split_labels)}]

각 분할에 맞는 운동 루틴을 반드시 아래 JSON 형식으로만 응답하세요. 추가 설명 없이 JSON만 반환하세요:
{{
  "routine_name": "루틴 이름",
  "days": [
    {{
      "day_label": "분할명",
      "exercises": [
        {{"name": "운동명", "sets": 4, "reps": 8, "weight_kg": 60.0}}
      ]
    }}
  ],
  "ai_comment": "2~3문장 동기부여 코멘트"
}}
"""
    raw = await call_claude(prompt)

    try:
        data = json.loads(raw)
        return RoutineGenerateResponse(**data)
    except (json.JSONDecodeError, ValueError, TypeError) as e:
        raise HTTPException(status_code=500, detail=f"AI 응답 파싱 실패: {e}")


@router.post("/adjust", response_model=RoutineAdjustResponse)
async def adjust_routine(req: RoutineAdjustRequest):
    """
    F904 - AI 루틴 조정
    세션 성공률을 분석해 운동별 강도 조정 (UP/HOLD/DOWN/VOLUME_UP/DELOAD)
    """
    adjustments = []
    for ex in req.exercises:
        action, new_weight, new_sets, new_reps = calculate_adjustment(
            ex.exercise_id, ex.target_sets, ex.target_reps,
            ex.target_weight_kg, req.recent_sessions,
        )
        adjustments.append(ExerciseAdjustment(
            exercise_id=ex.exercise_id,
            action=action,
            new_weight_kg=new_weight,
            new_sets=new_sets,
            new_reps=new_reps,
            reason=_REASON.get(action, action),
        ))

    ai_comment = await call_claude(
        f"루틴 조정 완료. {len(adjustments)}개 운동 분석. 격려 코멘트 한두 문장."
    )
    return RoutineAdjustResponse(
        adjustments=adjustments,
        ai_comment=ai_comment,
        next_week_number=req.current_week_number + 1,
    )
