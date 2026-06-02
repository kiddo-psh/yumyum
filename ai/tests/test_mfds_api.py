"""
식품안전처(MFDS) API 실제 연결 테스트
실행 방법:
  1. .env 파일에 MFDS_API_KEY 입력 후 ENV=prod 로 변경
  2. venv 활성화 상태에서:
     python tests/test_mfds_api.py

pytest가 아닌 python으로 직접 실행하는 파일입니다.
"""

import asyncio
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.config import settings
from app.services.mfds_service import search_food_mfds

MFDS_DIRECT_URL = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo02/getFoodNtrCpntDbInq02"


# ── 출력 헬퍼 ────────────────────────────────────────────
def ok(msg):       print(f"  ✅ {msg}")
def fail(msg):     print(f"  ❌ {msg}")
def info(msg):     print(f"  ℹ️  {msg}")
def section(title): print(f"\n{'='*50}\n  {title}\n{'='*50}")


# ── TEST 1: 환경 설정 확인 ───────────────────────────────
def test_config():
    section("TEST 1: 환경 설정 확인")

    info(f"ENV           : {settings.env}")
    key = settings.mfds_api_key
    info(f"MFDS_API_KEY  : {key[:8]}...")

    if settings.env != "prod":
        fail("ENV가 'prod'가 아닙니다 → .env 에서 ENV=prod 로 변경하세요")
        return False
    if key == "mock-key":
        fail("MFDS_API_KEY가 설정되지 않았습니다 → .env 에 실제 키를 입력하세요")
        return False

    ok("환경 설정 정상")
    return True


# ── TEST 2: MFDS API 직접 연결 ──────────────────────────
async def test_direct_connection():
    section("TEST 2: 식품안전처 API 직접 연결")

    import httpx
    params = {
        "serviceKey": settings.mfds_api_key,
        "pageNo":     1,
        "numOfRows":  3,
        "type":       "json",
        "FOOD_NM_KR": "닭가슴살",
    }
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            res = await client.get(MFDS_DIRECT_URL, params=params)
            res.raise_for_status()
            data = res.json()

        body = data.get("body", {})
        total = body.get("totalCount", 0)
        items = body.get("items", [])
        if isinstance(items, dict):
            items = [items]

        ok(f"HTTP 200 수신")
        ok(f"총 검색 결과: {total}건")
        for item in items[:3]:
            name = item.get("FOOD_NM_KR", "").replace("_", " ")
            info(f"  {name} | {item.get('AMT_NUM1')} kcal | 단백질 {item.get('AMT_NUM3')}g")
        return True
    except httpx.HTTPStatusError as e:
        fail(f"HTTP 오류 {e.response.status_code} — API 키를 확인하세요")
        return False
    except Exception as e:
        fail(f"연결 실패: {type(e).__name__}: {e}")
        return False


# ── TEST 3: FastAPI 엔드포인트 (MFDS 정상 결과) ──────────
async def test_search_with_results():
    section("TEST 3: GET /food/search — MFDS 결과 있는 검색어 (닭가슴살)")

    import httpx
    url = "http://localhost:8000/food/search"
    try:
        async with httpx.AsyncClient(timeout=15) as client:
            res = await client.get(url, params={"query": "닭가슴살", "page": 1, "size": 5})
            res.raise_for_status()
            data = res.json()

        ok(f"HTTP 200 수신")
        ok(f"총 결과: {data['total']}건  |  반환: {len(data['items'])}건")
        ok(f"query='{data['query']}', page={data['page']}, size={data['size']}")

        for item in data["items"]:
            info(f"  [{item['source']}] {item['name']} | {item['kcal']}kcal | 단백질 {item['protein_g']}g | 1회 {item['serving_size_g']}g")

        # 검증
        assert len(data["items"]) > 0,          "결과가 비어 있습니다"
        assert all(i["kcal"] > 0 for i in data["items"]),  "kcal이 0 이하인 항목 존재"
        assert all(i["source"] in ("MFDS", "AI") for i in data["items"]), "source 값 이상"
        ok("응답 구조 검증 통과")
        return True

    except httpx.ConnectError:
        fail("FastAPI 서버가 실행 중이지 않습니다 → uvicorn app.main:app --reload 먼저 실행")
        return False
    except AssertionError as e:
        fail(f"검증 실패: {e}")
        return False
    except Exception as e:
        fail(f"실패: {type(e).__name__}: {e}")
        return False


# ── TEST 4: AI fallback (DB에 없는 식품) ────────────────
async def test_search_ai_fallback():
    section("TEST 4: GET /food/search — MFDS 결과 없음 → AI fallback")

    import httpx
    # 식품안전처 DB에 없을 것 같은 식품명 (외국 음식)
    query = "파스타 알라 노르마"
    url = "http://localhost:8000/food/search"
    try:
        async with httpx.AsyncClient(timeout=30) as client:
            res = await client.get(url, params={"query": query})
            res.raise_for_status()
            data = res.json()

        ok(f"HTTP 200 수신")
        ok(f"query='{data['query']}' | 결과 {data['total']}건")

        if data["items"]:
            for item in data["items"]:
                info(f"  [{item['source']}] {item['name']} | {item['kcal']}kcal")
            sources = {i["source"] for i in data["items"]}
            if "AI" in sources:
                ok("AI fallback 동작 확인 (source=AI)")
            else:
                ok(f"MFDS에서 결과 반환됨 (source={sources})")
        else:
            info("결과 없음 (AI도 빈 응답) — Claude 응답 형식 확인 필요")

        return True

    except httpx.ConnectError:
        fail("FastAPI 서버가 실행 중이지 않습니다 → uvicorn app.main:app --reload 먼저 실행")
        return False
    except Exception as e:
        fail(f"실패: {type(e).__name__}: {e}")
        return False


# ── TEST 5: 페이지네이션 검증 ────────────────────────────
async def test_pagination():
    section("TEST 5: GET /food/search — 페이지네이션 검증")

    import httpx
    url = "http://localhost:8000/food/search"
    try:
        async with httpx.AsyncClient(timeout=15) as client:
            res1 = await client.get(url, params={"query": "닭", "page": 1, "size": 2})
            res2 = await client.get(url, params={"query": "닭", "page": 2, "size": 2})
            res1.raise_for_status()
            res2.raise_for_status()

        d1, d2 = res1.json(), res2.json()

        ok(f"page=1 결과: {[i['name'] for i in d1['items']]}")
        ok(f"page=2 결과: {[i['name'] for i in d2['items']]}")

        ids_p1 = {i["food_cd"] for i in d1["items"]}
        ids_p2 = {i["food_cd"] for i in d2["items"]}
        overlap = ids_p1 & ids_p2

        if overlap:
            fail(f"페이지 간 중복 항목 발생: {overlap}")
            return False

        ok("페이지 간 중복 없음 — 페이지네이션 정상")
        return True

    except httpx.ConnectError:
        fail("FastAPI 서버가 실행 중이지 않습니다 → uvicorn app.main:app --reload 먼저 실행")
        return False
    except Exception as e:
        fail(f"실패: {type(e).__name__}: {e}")
        return False


# ── TEST 6: 서비스 레이어 직접 호출 ─────────────────────
async def test_service_direct():
    section("TEST 6: mfds_service.search_food_mfds() 직접 호출")

    try:
        items, total = await search_food_mfds("계란", page=1, size=5)

        ok(f"총 {total}건 검색됨, {len(items)}건 반환")
        for item in items:
            info(f"  {item.food_cd} | {item.name} | {item.kcal}kcal | source={item.source}")

        assert all(item.protein_g >= 0 for item in items), "protein_g 음수 존재"
        assert all(item.serving_size_g > 0 for item in items), "serving_size_g 0 이하 존재"
        ok("데이터 유효성 검증 통과")
        return True
    except Exception as e:
        fail(f"실패: {type(e).__name__}: {e}")
        return False


# ── 메인 ─────────────────────────────────────────────────
async def main():
    print("\n🥗  냠냠코치 MFDS API 테스트 시작\n")

    results = {}

    results["환경설정"] = test_config()
    if not results["환경설정"]:
        print("\n⚠️  환경 설정을 먼저 완료하세요.")
        print("  ENV=prod, MFDS_API_KEY=발급받은키 로 .env 수정 후 재실행\n")
        return

    results["MFDS직접연결"] = await test_direct_connection()
    results["서비스레이어"]  = await test_service_direct()
    results["검색_정상"]    = await test_search_with_results()
    results["AI_fallback"]  = await test_search_ai_fallback()
    results["페이지네이션"] = await test_pagination()

    section("테스트 결과 요약")
    for name, result in results.items():
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"  {status}  {name}")

    passed = sum(1 for r in results.values() if r)
    print(f"\n  총 {passed}/{len(results)} 통과\n")


if __name__ == "__main__":
    asyncio.run(main())
