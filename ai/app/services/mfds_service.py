import httpx
from app.config import settings
from app.schemas.food import FoodItem

MFDS_URL = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo02/getFoodNtrCpntDbInq02"

# ── dev 모드용 mock DB ──────────────────────────────────────────────────────────
_MOCK_DB: dict[str, list[dict]] = {
    "닭가슴살": [
        {"food_cd": "D001001", "name": "닭가슴살(구이)",  "serving_size_g": 100, "kcal": 165, "protein_g": 31.0, "carb_g": 0.0,  "fat_g": 3.6,  "sugar_g": 0.0, "fiber_g": 0.0},
        {"food_cd": "D001002", "name": "닭가슴살(삶은)",  "serving_size_g": 100, "kcal": 158, "protein_g": 28.9, "carb_g": 0.0,  "fat_g": 3.2,  "sugar_g": 0.0, "fiber_g": 0.0},
    ],
    "닭": [
        {"food_cd": "D001001", "name": "닭가슴살(구이)",  "serving_size_g": 100, "kcal": 165, "protein_g": 31.0, "carb_g": 0.0,  "fat_g": 3.6,  "sugar_g": 0.0, "fiber_g": 0.0},
        {"food_cd": "D001003", "name": "닭다리(구이)",    "serving_size_g": 100, "kcal": 218, "protein_g": 25.9, "carb_g": 0.0,  "fat_g": 12.4, "sugar_g": 0.0, "fiber_g": 0.0},
    ],
    "현미밥": [
        {"food_cd": "D002001", "name": "현미밥",          "serving_size_g": 210, "kcal": 341, "protein_g": 7.1,  "carb_g": 72.9, "fat_g": 2.1,  "sugar_g": 0.0, "fiber_g": 3.8},
    ],
    "고구마": [
        {"food_cd": "D003001", "name": "고구마(구운)",    "serving_size_g": 150, "kcal": 171, "protein_g": 2.6,  "carb_g": 40.0, "fat_g": 0.3,  "sugar_g": 10.1, "fiber_g": 3.6},
        {"food_cd": "D003002", "name": "고구마(삶은)",    "serving_size_g": 150, "kcal": 149, "protein_g": 2.3,  "carb_g": 35.0, "fat_g": 0.2,  "sugar_g": 8.2,  "fiber_g": 3.1},
    ],
    "계란": [
        {"food_cd": "D004001", "name": "계란(삶은)",      "serving_size_g": 50,  "kcal": 74,  "protein_g": 6.3,  "carb_g": 0.6,  "fat_g": 5.0,  "sugar_g": 0.0, "fiber_g": 0.0},
        {"food_cd": "D004002", "name": "계란(스크램블)",  "serving_size_g": 60,  "kcal": 91,  "protein_g": 6.1,  "carb_g": 1.0,  "fat_g": 6.7,  "sugar_g": 0.0, "fiber_g": 0.0},
    ],
    "두부": [
        {"food_cd": "D005001", "name": "두부(순두부)",    "serving_size_g": 200, "kcal": 98,  "protein_g": 9.6,  "carb_g": 3.0,  "fat_g": 5.0,  "sugar_g": 0.0, "fiber_g": 0.4},
        {"food_cd": "D005002", "name": "두부(부침)",      "serving_size_g": 120, "kcal": 100, "protein_g": 8.6,  "carb_g": 2.4,  "fat_g": 5.8,  "sugar_g": 0.0, "fiber_g": 0.3},
    ],
    "연어": [
        {"food_cd": "D006001", "name": "연어(구이)",      "serving_size_g": 100, "kcal": 208, "protein_g": 20.4, "carb_g": 0.0,  "fat_g": 13.4, "sugar_g": 0.0, "fiber_g": 0.0},
        {"food_cd": "D006002", "name": "연어(생)",        "serving_size_g": 100, "kcal": 179, "protein_g": 19.8, "carb_g": 0.0,  "fat_g": 10.7, "sugar_g": 0.0, "fiber_g": 0.0},
    ],
    "바나나": [
        {"food_cd": "D007001", "name": "바나나",          "serving_size_g": 100, "kcal": 89,  "protein_g": 1.1,  "carb_g": 23.0, "fat_g": 0.3,  "sugar_g": 12.2, "fiber_g": 2.6},
    ],
    "아보카도": [
        {"food_cd": "D008001", "name": "아보카도",        "serving_size_g": 100, "kcal": 160, "protein_g": 2.0,  "carb_g": 8.5,  "fat_g": 14.7, "sugar_g": 0.7,  "fiber_g": 6.7},
    ],
    "오트밀": [
        {"food_cd": "D009001", "name": "오트밀(조리)",    "serving_size_g": 240, "kcal": 166, "protein_g": 5.9,  "carb_g": 28.2, "fat_g": 3.6,  "sugar_g": 0.0, "fiber_g": 4.0},
    ],
    "그릭요거트": [
        {"food_cd": "D010001", "name": "그릭요거트(무가당)", "serving_size_g": 170, "kcal": 100, "protein_g": 17.0, "carb_g": 6.0,  "fat_g": 0.7,  "sugar_g": 4.0,  "fiber_g": 0.0},
    ],
}


def _mock_search(query: str, page: int, size: int) -> tuple[list[FoodItem], int]:
    """키워드 포함 여부로 mock DB 검색. 중복 제거 후 페이지네이션 적용."""
    seen: set[str] = set()
    results: list[FoodItem] = []

    for keyword, items in _MOCK_DB.items():
        if keyword in query:
            for raw in items:
                if raw["food_cd"] not in seen:
                    seen.add(raw["food_cd"])
                    results.append(FoodItem(**raw, source="MFDS"))

    total = len(results)
    start = (page - 1) * size
    return results[start : start + size], total


# ── 실 API 응답 파싱 ───────────────────────────────────────────────────────────
def _safe_float(value, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _parse_mfds(data: dict, page: int, size: int) -> tuple[list[FoodItem], int]:
    # FoodNtrCpntDbInfo02 실제 응답 구조: body.items 가 바로 list로 내려옴
    # 문서 스키마(items.item 중첩) 와 다르므로 두 형태 모두 대응
    body = data.get("body", {})
    total = int(body.get("totalCount", 0) or 0)

    items_container = body.get("items", [])
    if isinstance(items_container, list):
        raw_items = items_container
    elif isinstance(items_container, dict):
        raw_items = items_container.get("item", [])
        if isinstance(raw_items, dict):
            raw_items = [raw_items]
    else:
        raw_items = []

    # FoodNtrCpntDbInfo02 영양소 필드 매핑
    # AMT_NUM1=에너지(kcal)  AMT_NUM3=단백질(g)  AMT_NUM4=지방(g)
    # AMT_NUM6=탄수화물(g)   AMT_NUM7=당류(g)    AMT_NUM8=식이섬유(g)
    items: list[FoodItem] = []
    for item in raw_items:
        raw_name = str(item.get("FOOD_NM_KR", ""))
        clean_name = raw_name.replace("_", " ").strip()

        items.append(FoodItem(
            food_cd        = str(item.get("FOOD_CD", "")),
            name           = clean_name,
            serving_size_g = _safe_float(item.get("SERVING_SIZE"), 100.0),
            kcal           = _safe_float(item.get("AMT_NUM1")),
            protein_g      = _safe_float(item.get("AMT_NUM3")),
            fat_g          = _safe_float(item.get("AMT_NUM4")),
            carb_g         = _safe_float(item.get("AMT_NUM6")),
            sugar_g        = _safe_float(item.get("AMT_NUM7")),
            fiber_g        = _safe_float(item.get("AMT_NUM8")),
            source         = "MFDS",
        ))

    return items, total


# ── 공개 인터페이스 ────────────────────────────────────────────────────────────
async def search_food_mfds(query: str, page: int = 1, size: int = 10) -> tuple[list[FoodItem], int]:
    """
    식품안전처 영양성분 DB 검색.
    dev 모드 또는 API 키 미설정 시 mock 반환.
    """
    if settings.env == "dev" or settings.mfds_api_key == "mock-key":
        return _mock_search(query, page, size)

    params = {
        "serviceKey": settings.mfds_api_key,
        "pageNo":     page,
        "numOfRows":  size,
        "type":       "json",
        "FOOD_NM_KR": query,
    }
    async with httpx.AsyncClient(timeout=10) as client:
        res = await client.get(MFDS_URL, params=params)
        res.raise_for_status()

    return _parse_mfds(res.json(), page, size)
