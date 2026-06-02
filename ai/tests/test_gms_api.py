"""
GMS API 실제 연결 테스트
실행 방법:
  1. .env 파일에 GMS_API_KEY 입력 후 ENV=prod 로 변경
  2. venv 활성화 상태에서:
     python tests/test_gms_api.py

pytest가 아닌 python으로 직접 실행하는 파일입니다.
"""

import asyncio
import json
import sys
import os

# fastapi-server/ 루트를 경로에 추가 (app 모듈 인식용)
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.config import settings
from app.services.claude_service import call_claude


# ── 색상 출력 헬퍼 ──────────────────────────────────────
def ok(msg):   print(f"  ✅ {msg}")
def fail(msg): print(f"  ❌ {msg}")
def info(msg): print(f"  ℹ️  {msg}")
def section(title): print(f"\n{'='*50}\n  {title}\n{'='*50}")


# ── 테스트 1: 환경 설정 확인 ────────────────────────────
def test_config():
    section("TEST 1: 환경 설정 확인")

    info(f"ENV         : {settings.env}")
    info(f"GMS_BASE_URL: {settings.gms_base_url}")
    info(f"DEFAULT_MODEL: {settings.default_model}")

    key_preview = settings.gms_api_key[:8] + "..." if len(settings.gms_api_key) > 8 else settings.gms_api_key
    info(f"GMS_API_KEY : {key_preview}")

    if settings.env != "prod":
        fail("ENV가 'prod'가 아닙니다 → .env 파일에서 ENV=prod 로 변경하세요")
        return False

    if settings.gms_api_key == "mock-key":
        fail("GMS_API_KEY가 설정되지 않았습니다 → .env 파일에 실제 키를 입력하세요")
        return False

    ok("환경 설정 정상")
    return True


# ── 테스트 2: GMS API 기본 연결 ─────────────────────────
async def test_basic_connection():
    section("TEST 2: GMS API 기본 연결 (Hi Claude)")

    try:
        response = await call_claude("안녕하세요! 한 문장으로 짧게 인사해주세요.")
        ok(f"응답 수신 성공")
        info(f"응답 내용: {response[:100]}")
        return True
    except Exception as e:
        fail(f"연결 실패: {e}")
        return False


# ── 테스트 3: F206 온보딩 AI 플랜 생성 ──────────────────
async def test_plan_generate():
    section("TEST 3: F206 온보딩 AI 플랜 생성")

    import httpx
    url = "http://localhost:8000/ai/plan/generate"
    payload = {
        "gender": "M",
        "age": 25,
        "height_cm": 175,
        "weight_kg": 70,
        "activity_level": "moderate",
        "health_goal": "MUSCLE_GAIN"
    }

    try:
        async with httpx.AsyncClient(timeout=30) as client:
            res = await client.post(url, json=payload)
            res.raise_for_status()
            data = res.json()

        ok(f"BMR       : {data['bmr']} kcal")
        ok(f"TDEE      : {data['tdee']} kcal")
        ok(f"목표 칼로리: {data['target_kcal']} kcal")
        ok(f"단백질    : {data['target_protein_g']} g")
        ok(f"탄수화물  : {data['target_carb_g']} g")
        ok(f"지방      : {data['target_fat_g']} g")
        ok(f"AI 코멘트 : {data['ai_comment']}...")
        return True
    except httpx.ConnectError:
        fail("FastAPI 서버가 실행 중이지 않습니다 → uvicorn app.main:app --reload 먼저 실행")
        return False
    except Exception as e:
        fail(f"실패: {e}")
        return False


# ── 테스트 4: F802 마지막 끼니 추천 ─────────────────────
async def test_last_recommend():
    section("TEST 4: F802 마지막 끼니 추천")

    import httpx
    url = "http://localhost:8000/ai/meal/last-recommend"
    payload = {
        "total_kcal": 1200,
        "total_protein_g": 75,
        "total_carb_g": 150,
        "total_fat_g": 30,
        "target_kcal": 2000,
        "target_protein_g": 150,
        "target_carb_g": 225,
        "target_fat_g": 55,
        "meal_count": 2
    }

    try:
        async with httpx.AsyncClient(timeout=30) as client:
            res = await client.post(url, json=payload)
            res.raise_for_status()
            data = res.json()

        ok(f"우선 영양소: {data['priority_nutrient']}")
        ok(f"AI 코멘트 : {data['ai_comment']}")
        ok(f"추천 식단 {len(data['recommendations'])}개:")
        for i, r in enumerate(data['recommendations'], 1):
            info(f"  {i}. {r['name']} | {r['kcal']}kcal | 단백질 {r['protein_g']}g | 이유: {r['reason']}")
        return True
    except httpx.ConnectError:
        fail("FastAPI 서버가 실행 중이지 않습니다 → uvicorn app.main:app --reload 먼저 실행")
        return False
    except Exception as e:
        fail(f"실패: {e}")
        return False


# ── 테스트 5: Claude 직접 프롬프트 테스트 ───────────────
async def test_claude_direct():
    section("TEST 5: Claude 직접 프롬프트 — 식단 추천")

    prompt = """
잔여 영양소: 단백질 60g, 탄수화물 60g, 지방 20g, 칼로리 700kcal.
잔여 영양소를 대부분 채울 수 있는 현실적인 저녁 식단 3가지를 추천해줘.
JSON 배열로만 응답해:
[{"name": "음식명", "kcal": 숫자, "protein_g": 숫자, "carb_g": 숫자, "fat_g": 숫자, "reason": "이유"}]
"""

    try:
        response = await call_claude(prompt)
        ok("Claude 응답 수신")
        info(f"원본 응답:\n{response[:300]}")

        # JSON 파싱 시도
        try:
            cleaned = response.strip()
            if "```" in cleaned:
                cleaned = cleaned.split("```")[1]
                if cleaned.startswith("json"):
                    cleaned = cleaned[4:]
            meals = json.loads(cleaned)
            ok(f"JSON 파싱 성공 — {len(meals)}개 식단")
            for m in meals:
                info(f"  • {m['name']} ({m['kcal']}kcal)")
        except json.JSONDecodeError:
            info("JSON 파싱 실패 (응답 형식 확인 필요) — 응답 자체는 수신됨")

        return True
    except Exception as e:
        fail(f"실패: {e}")
        return False


# ── 메인 실행 ────────────────────────────────────────────
async def main():
    print("\n🍽️  냠냠코치 GMS API 테스트 시작\n")

    results = {}

    # 1. 환경 설정 (동기)
    results["환경설정"] = test_config()
    if not results["환경설정"]:
        print("\n⚠️  환경 설정을 먼저 완료하세요. 나머지 테스트를 건너뜁니다.")
        print("\n[ .env 파일 설정 방법 ]")
        print("  GMS_API_KEY=발급받은_키_입력")
        print("  ENV=prod\n")
        return

    # 2. GMS API 직접 연결
    results["GMS연결"] = await test_basic_connection()

    # 3~4. FastAPI 서버 필요한 테스트
    results["플랜생성"] = await test_plan_generate()
    results["끼니추천"] = await test_last_recommend()

    # 5. Claude 직접 프롬프트
    results["Claude직접"] = await test_claude_direct()

    # ── 결과 요약 ──
    section("테스트 결과 요약")
    for name, result in results.items():
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"  {status}  {name}")

    passed = sum(1 for r in results.values() if r)
    print(f"\n  총 {passed}/{len(results)} 통과\n")


if __name__ == "__main__":
    asyncio.run(main())
