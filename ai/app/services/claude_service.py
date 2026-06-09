import json
import httpx
from app.config import settings


async def call_claude(prompt: str, model: str | None = None, max_tokens: int = 1000) -> str:
    """
    GMS API를 통해 Claude 호출.
    dev 환경에서는 mock 응답 반환 (크레딧 절약).
    """
    if settings.env == "dev":
        return _mock_response(prompt)

    return await _call_gms(prompt, model=model or settings.default_model, max_tokens=max_tokens)


async def _call_gms(prompt: str, model: str, max_tokens: int) -> str:
    """
    GMS(Gen AI Management System) API 호출.
    curl 형식:
      POST https://gms.ssafy.io/gmsapi/api.anthropic.com/v1/messages
      x-api-key: $GMS_KEY
      anthropic-version: 2023-06-01
    """
    url = f"{settings.gms_base_url}/v1/messages"
    headers = {
        "Content-Type": "application/json",
        "x-api-key": settings.gms_api_key,
        "anthropic-version": settings.anthropic_version,
    }
    payload = {
        "model": model,
        "max_tokens": max_tokens,
        "messages": [{"role": "user", "content": prompt}],
    }

    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.post(url, headers=headers, json=payload)
        response.raise_for_status()

    data = response.json()
    return data["content"][0]["text"]


def _mock_response(prompt: str) -> str:
    """개발용 mock 응답 (크레딧 절약). JSON 요청 여부에 따라 형식 분리."""
    if "루틴" in prompt or "routine" in prompt.lower():
        return json.dumps({
            "routine_name": "4일 상체/하체 분할 루틴",
            "days": [
                {
                    "day_label": "상체",
                    "exercises": [
                        {"name": "벤치프레스", "sets": 4, "reps": 8, "weight_kg": 60.0},
                        {"name": "덤벨 숄더프레스", "sets": 3, "reps": 10, "weight_kg": 18.0},
                        {"name": "랫풀다운", "sets": 3, "reps": 10, "weight_kg": 45.0}
                    ]
                },
                {
                    "day_label": "하체",
                    "exercises": [
                        {"name": "바벨 스쿼트", "sets": 4, "reps": 8, "weight_kg": 80.0},
                        {"name": "레그프레스", "sets": 3, "reps": 12, "weight_kg": 120.0},
                        {"name": "루마니안 데드리프트", "sets": 3, "reps": 10, "weight_kg": 60.0}
                    ]
                },
                {
                    "day_label": "상체",
                    "exercises": [
                        {"name": "인클라인 벤치프레스", "sets": 3, "reps": 10, "weight_kg": 50.0},
                        {"name": "바벨 로우", "sets": 4, "reps": 8, "weight_kg": 55.0},
                        {"name": "바벨 컬", "sets": 3, "reps": 12, "weight_kg": 25.0}
                    ]
                },
                {
                    "day_label": "하체",
                    "exercises": [
                        {"name": "핵 스쿼트", "sets": 4, "reps": 10, "weight_kg": 70.0},
                        {"name": "레그 컬", "sets": 3, "reps": 12, "weight_kg": 40.0},
                        {"name": "카프레이즈", "sets": 4, "reps": 15, "weight_kg": 0.0}
                    ]
                }
            ],
            "ai_comment": "근육량 증가를 위해 복합운동 위주로 구성했습니다. 점진적으로 무게를 늘려보세요!"
        }, ensure_ascii=False)

    if "JSON" in prompt or "json" in prompt:
        return (
            '[{"name":"닭가슴살 샐러드","kcal":380,"protein_g":42,"carb_g":18,"fat_g":12,'
            '"reason":"단백질 보충에 최적"},'
            '{"name":"두부된장찌개+현미밥","kcal":420,"protein_g":22,"carb_g":58,"fat_g":9,'
            '"reason":"균형잡힌 한식"},'
            '{"name":"연어구이+고구마","kcal":390,"protein_g":35,"carb_g":32,"fat_g":14,'
            '"reason":"오메가3 + 복합 탄수화물"}]'
        )
    return "오늘 하루도 균형 잡힌 식단으로 건강 목표에 가까워지고 있어요! 단백질 섭취에 특히 신경 써보세요."
