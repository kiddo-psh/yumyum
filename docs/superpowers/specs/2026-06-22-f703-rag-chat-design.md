# F703 RAG 영양 채팅 설계 문서

**날짜:** 2026-06-22
**담당:** FastAPI
**우선순위:** 심화

---

## 1. 개요

사용자가 영양 관련 질문을 자유롭게 입력하면, 식품안전처 DB 기반 지식 베이스에서 관련 식품 정보를 검색(RAG)하고, 개인 식단 데이터를 컨텍스트로 주입해 Claude가 출처 명시 답변을 반환하는 채팅 기능.

**엔드포인트:** `POST /ai/chat`
**호출 흐름:** Vue → Spring Boot → FastAPI

---

## 2. 주요 질문 유형

| 유형 | 예시 | 처리 |
|---|---|---|
| 식품 정보 조회 | "닭가슴살 100g 단백질 얼마야?" | RAG 검색 |
| 영양소 추천 | "철분 많은 음식 뭐야?" | RAG 검색 |
| 개인 식단 분석 | "오늘 단백질 부족한가요?" | context 주입 + RAG |
| 목표 달성 | "오늘 목표 칼로리 맞출 수 있어?" | context 주입 |
| 일반 영양 지식 | "다이어트 중에 과일 먹어도 돼?" | Claude 자체 지식 |

---

## 3. 아키텍처

### 요청 처리 흐름

```
사용자 질문 + 식단 context (optional)
    ↓
POST /ai/chat (FastAPI)
    ↓
ChromaDB 검색 (질문 → 관련 식품 문서 3개)
    ↓
프롬프트 조립
  [시스템 지시] + [검색 식품 정보] + [개인 식단 context] + [질문]
    ↓
Claude 호출 (GMS API)
    ↓
{ answer, sources } 반환
```

### 지식 베이스 초기화 (앱 시작 시 1회)

```
nutrition_kb.json (식품 300개 번들)
    ↓ app startup
ChromaDB 적재 (이미 있으면 스킵)
```

---

## 4. 스키마

### 요청
```json
{
  "message": "오늘 단백질 부족한가요?",
  "context": {
    "total_protein_g": 39.0,
    "target_protein_g": 120.0,
    "total_carb_g": 150.0,
    "target_carb_g": 250.0,
    "total_fat_g": 30.0,
    "target_fat_g": 60.0,
    "total_kcal": 900.0,
    "target_kcal": 2000.0,
    "health_goal": "MUSCLE"
  }
}
```

- `context`: optional. 식품 정보 질문 시 null로 전송.
- `health_goal`: `DIET | MUSCLE | HEALTH | DISEASE`

### 응답
```json
{
  "answer": "오늘 단백질 섭취량이 목표의 32%입니다. 닭가슴살이나 두부를 추가하면 좋습니다.",
  "sources": [
    { "name": "닭가슴살", "info": "100g당 단백질 23g" },
    { "name": "두부",     "info": "100g당 단백질 8g" }
  ]
}
```

- `sources`: ChromaDB에서 검색된 식품. 검색 결과 없으면 빈 배열.

---

## 5. 파일 구조

```
ai/app/
├── services/
│   ├── rag_service.py      # ChromaDB 초기화·검색
│   └── chat_service.py     # 프롬프트 조립 + Claude 호출
├── routers/
│   └── ai_chat.py          # POST /ai/chat
├── schemas/
│   └── chat.py             # ChatRequest, ChatResponse
└── data/
    └── nutrition_kb.json   # 식품 지식 베이스 (300개)
```

---

## 6. 핵심 구현 명세

### rag_service.py

```python
# ChromaDB 설정
- client: PersistentClient(path="./chroma_db")
- collection: "nutrition"
- embedding_function: SentenceTransformerEmbeddingFunction(
    model_name="paraphrase-multilingual-MiniLM-L12-v2"
  )

# 초기화
- nutrition_kb.json 로드
- collection이 비어 있으면 전체 문서 add()
- 이미 데이터 있으면 스킵

# 검색
- search(query: str, n_results: int = 3) -> list[dict]
- 반환: [{ "name": str, "document": str, "info": str }]
```

### nutrition_kb.json 문서 형식

```json
[
  {
    "id": "food_001",
    "name": "닭가슴살",
    "document": "닭가슴살 100g: 열량 109kcal, 단백질 23g, 지방 2g, 탄수화물 0g. 고단백 저지방 식품으로 다이어트·근육 증가에 적합."
  }
]
```

자연어 문장 형식으로 구성해 임베딩 품질을 높인다.

### chat_service.py 프롬프트 구조

```
당신은 영양 전문 AI 코치입니다. 아래 식품 정보와 사용자 데이터를 바탕으로 답변하세요.

[참고 식품 정보]
- 닭가슴살: 100g당 단백질 23g, 열량 109kcal
- 두부: 100g당 단백질 8g, 열량 76kcal
- 현미밥: 100g당 탄수화물 34g, 열량 156kcal

[사용자 오늘 식단] (context가 있을 때만 포함)
- 섭취 단백질: 39g / 목표: 120g
- 섭취 칼로리: 900kcal / 목표: 2000kcal
- 건강 목표: 근육 증가

[질문]
오늘 단백질 부족한가요?
```

### ai_chat.py 라우터

```python
@router.post("/ai/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    try:
        docs = rag_service.search(req.message)
    except Exception:
        docs = []  # RAG 실패 시 빈 결과로 계속 진행

    if settings.env == "dev":
        return _mock_response(req.message)

    prompt = chat_service.build_prompt(req.message, docs, req.context)
    answer = await call_claude(prompt, max_tokens=500)
    sources = [{"name": d["name"], "info": d["info"]} for d in docs]
    return ChatResponse(answer=answer, sources=sources)
```

---

## 7. 에러 처리

| 상황 | 처리 방식 |
|---|---|
| ChromaDB 검색 실패 | `sources=[]`로 Claude만 호출, 500 방지 |
| Claude 호출 실패 | HTTPException 500, 기존 fallback 패턴 동일 |
| context 없는 식품 질문 | RAG 결과만으로 정상 응답 |

---

## 8. Mock 모드

`ENV=dev`일 때 ChromaDB·Claude 호출 없이 즉시 반환.

```python
def _mock_response(message: str) -> ChatResponse:
    return ChatResponse(
        answer="[MOCK] 닭가슴살과 두부를 추가하면 단백질 목표 달성에 도움이 됩니다.",
        sources=[
            {"name": "닭가슴살", "info": "100g당 단백질 23g"},
            {"name": "두부", "info": "100g당 단백질 8g"},
        ]
    )
```

---

## 9. 의존성 추가

```
# requirements.txt에 추가
chromadb==0.5.23
sentence-transformers==3.3.1
```

---

## 10. 완료 기준 (DoD)

- `POST /ai/chat` 호출 시 `answer` + `sources` JSON 반환
- "닭가슴살 단백질 얼마야?" 질문 → sources에 닭가슴살 포함
- "오늘 단백질 부족한가요?" + context → 개인 수치 기반 답변
- `ENV=dev` mock 모드 정상 동작
- pytest 테스트 작성
