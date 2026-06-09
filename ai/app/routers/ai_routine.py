import json
from fastapi import APIRouter, HTTPException
from app.schemas.routine import RoutineGenerateRequest, RoutineGenerateResponse
from app.services.claude_service import call_claude

router = APIRouter(prefix="/ai/routine", tags=["AI Routine"])


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
