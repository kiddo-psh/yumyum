# 냠냠코치 4주 개발 로드맵 v3

**2인 팀**

**기술 스택**: Spring Boot (Java) + FastAPI (Python / AI) + Vue 3 (Frontend)

**변경 요약**: 코인 시스템 삭제 / F802 → 마지막 끼니 추천 기능으로 변경

---

## v3 변경 내역

v2 로드맵 대비 수정된 항목입니다.

| 유형 | 번호 | 기능명 | 변경 내용 |
|---|---|---|---|
| 삭제 | G701 | Coin 시스템 | DailyGoal·Streak 기반 코인 지급 전면 삭제. 뱃지·Nyam 성장으로 동기부여 통합 |
| 삭제 | G301 일부 | Coin 지급 로직 | XP·Coin 동시 지급 구조 삭제. Streak·뱃지만 남김 |
| 수정 | F802 | 운동 후 식단 추천 → 마지막 끼니 추천 | 운동 완료 트리거 제거. 한 끼 분량 칼로리 잔여 시 남은 영양소 기반 식단 추천으로 변경 |

---

## F802 — 마지막 끼니 추천 기능 설계 (변경)

### 개요

하루 중 한 끼 분량의 칼로리가 남았을 때, 남은 영양소(단백질·탄수화물·지방) 그램 수를 분석하여 해당 영양소를 채울 수 있는 실제 식단 조합을 추천합니다.

### 트리거 조건

| 조건 | 기준값 | 설명 |
|---|---|---|
| 잔여 칼로리 | 1끼 칼로리 ± 20% | 목표 칼로리 ÷ 끼니 수 기준. 예) 2000kcal / 3끼 → 600~800kcal 남았을 때 트리거 |
| 시간대 | 오후 5시 이후 | 저녁 끼니 추천에만 발동. 낮에는 노출하지 않음 |
| 당일 Meal 기록 | 최소 1끼 이상 기록됨 | 식단 기록이 없으면 영양소 잔여량 계산 불가 → 추천 미발동 |

### FastAPI 처리 흐름

| 단계 | 처리 내용 | 담당 |
|---|---|---|
| ① | Spring → FastAPI 호출: 오늘 섭취 영양소 합계 전달 (`total_kcal`, `protein_g`, `carb_g`, `fat_g`, `target_kcal`, `target_protein`, `target_carb`, `target_fat`) | Spring Boot |
| ② | 잔여 영양소 계산: `remain_protein = target_protein - total_protein`, `remain_carb = target_carb - total_carb`, `remain_fat = target_fat - total_fat`, `remain_kcal = target_kcal - total_kcal` | FastAPI |
| ③ | 부족 영양소 우선순위 판단: 가장 부족 비율이 높은 영양소를 1순위로 설정. 예) 단백질 달성률 40%, 탄수 75%, 지방 80% → 단백질 우선 | FastAPI |
| ④ | Claude API로 식단 3개 추천 생성: 잔여 칼로리·각 영양소 그램 수를 프롬프트에 포함. 각 추천에 음식명·칼로리·단백질·탄수화물·지방 포함 | FastAPI + Claude |
| ⑤ | Spring이 결과 수신 → Vue 프론트에 전달. “오늘 영양소를 채울 마지막 끼니 추천” 카드로 홈 화면 노출 | Spring + Vue |

### FastAPI 엔드포인트

| 항목 | 내용 |
|---|---|
| 엔드포인트 | `POST /ai/meal/last-recommend` |
| 요청 Body | `total_kcal`, `total_protein_g`, `total_carb_g`, `total_fat_g`, `target_kcal`, `target_protein_g`, `target_carb_g`, `target_fat_g`, `meal_count` (오늘 기록된 끼니 수) |
| 응답 | `recommendations: [ { name, kcal, protein_g, carb_g, fat_g, reason }, // 3개 ]`<br>`priority_nutrient: "protein" \| "carb" \| "fat"`<br>`ai_comment: str` |
| Claude 프롬프트 방향 | 잔여 단백질 Xg, 탄수화물 Xg, 지방 Xg, 칼로리 Xkcal를 채울 수 있는 실제 한식 기반 식단 3가지를 추천. 각 식단의 영양소 수치 포함. 과도하게 많은 양이 아닌 현실적인 한 끼 분량으로 제안. |

**예시 추천 결과**

- 닭가슴살 샐러드(380kcal / 단백질 42g / 탄수 18g / 지방 12g)
- 두부된장찌개 + 현미밥(420kcal / 단백질 22g / 탄수 58g / 지방 9g)
- 연어구이 + 고구마(390kcal / 단백질 35g / 탄수 32g / 지방 14g)

---

## 전체 MVP 로드맵 요약 (v3)

|  | 1주차 | 2주차 | 3주차 | 4주차 |
|---|---|---|---|---|
| MVP | 1차 MVP | 1차 MVP 마무리 + 2차 시작 | 2차 MVP | 3차 MVP + 통합 QA |
| 핵심 | Member·Meal·Program<br>DailyGoal·Streak·AI플랜 | WeeklyReport·운동루틴<br>마지막끼니추천·Nyam | 커뮤니티·대시보드<br>AI식단·운동코칭·뱃지 | RAG채팅·Multi-Agent<br>Nyam완성·통합QA |

---

## 1주차 1차 MVP — 핵심 뼈대

**범위**: Member · Meal · Program · DailyGoal · Streak · AI 플랜 생성

**목표**: 가입 → 온보딩 → 식단 기록 → Streak 확인 핵심 루프 완성

| Spring Boot (Java) | FastAPI (Python / AI) |
|---|---|
| **환경 설정**<br>- Docker Compose 전체 실행 확인<br>- Spring Boot 프로젝트 생성<br>- JWT 인증 필터 기본 구조<br>- `application.yml` 환경별 분리 | **환경 설정**<br>- `fastapi-server/` 폴더 구조 정리<br>- `config.py` `.env` 로딩<br>- `/health` 엔드포인트<br>- `claude_service.py` mock 모드 |
| **Member**<br>- F201 회원가입 (HealthGoal·신체정보)<br>- F202/203/204 조회·수정·비활성화<br>- F205 로그인·로그아웃 (JWT + Redis) | **온보딩 AI 플랜 (F206)**<br>- `POST /ai/plan/generate` — BMR·TDEE 계산<br>- 목표별 칼로리·영양소 비율 설정<br>- Claude API 개인화 코멘트 생성 |
| **Meal**<br>- F101 Meal 작성 (MealItem + Food 검색)<br>- F102/103/104 조회·수정·삭제<br>- F105 식단 분석 — 칼로리·영양소 합산 | **Food DB**<br>- 식품안전처 공공데이터 API 연동<br>- `GET /food/search` 엔드포인트<br>- MFDS·MANUAL·AI source 구분 |
| **Program + DailyGoal**<br>- 온보딩 완료 시 Program 생성 (FastAPI 호출)<br>- DailyGoal 생성·달성 여부 업데이트<br>- Program 없을 때 시스템 기본값 적용 |  |
| **Streak (Coin 없음)**<br>- G201 Streak 카운트 (DailyGoal 기준)<br>- G203 Streak 알림 트리거<br>- Coin 지급 로직 없음 — 뱃지·Nyam으로만 |  |

### 1주차 산출물

| 구분 | 산출물 |
|---|---|
| API | `POST /auth/signup`, `POST /auth/login`, `GET·PUT /members/me` |
| API | `POST·GET·PUT·DELETE /meals`, `GET /meals/daily-summary` |
| API | `POST /programs` (FastAPI 연동), `GET /streak` |
| API | `POST /ai/plan/generate` (FastAPI), `GET /food/search` |
| 인프라 | `docker compose up -d` 전체 실행 가능 상태 |

### 1주차 완료 기준

- 회원가입 → 온보딩 → 식단 기록 → Streak 조회 흐름 Swagger 수동 테스트 통과
- FastAPI `/ai/plan/generate` 호출 시 칼로리·영양소 JSON 반환 확인

---

## 2주차 1차 마무리 + 2차 시작

**범위**: 운동 루틴 시스템 (F901~F904) · WeeklyReport · 마지막 끼니 추천(F802) · Nyam

**목표**: 운동 루틴 시스템 완성 + 마지막 끼니 추천(F802) 구현. AI 코칭 첫 경험 제공

| Spring Boot (Java) | FastAPI (Python / AI) |
|---|---|
| **1차 MVP 마무리**<br>- G501 주간 달성 캘린더 API<br>- G502 목표 달성 진행바 API<br>- 마지막 끼니 추천 트리거 조건 체크 로직 (잔여칼로리 1끼 분량 ± 20% + 오후 5시 이후) | **운동 루틴 AI (F902·F904)**<br>- `POST /ai/routine/generate` — 루틴 없는 사용자 추천 루틴<br>- `POST /ai/routine/generate` — 기존 루틴 점진 계획 생성<br>- `POST /ai/routine/adjust` — 성공/실패 → UP/HOLD/DOWN/DELOAD<br>- `GET /ai/routine/weekly-plan/{id}/{week}` |
| **운동 루틴 등록 (F901)**<br>- 루틴 타입 구분 (EXISTING / NEW)<br>- 기존 루틴 입력 저장 (운동명·세트·무게·반복)<br>- FastAPI 루틴 생성 결과 수신·저장 | **마지막 끼니 추천 (F802)**<br>- `POST /ai/meal/last-recommend`<br>- 잔여 영양소 계산 (단백질·탄수·지방 그램 수)<br>- 부족 영양소 우선순위 판단<br>- Claude API 식단 3개 추천 생성 (음식명·kcal·단백질·탄수·지방 포함)<br>- 한식 기반 현실적인 한 끼 분량 제안 |
| **루틴 기록 (F903)**<br>- 세트 단위 성공/실패 기록 API<br>- 세션 완료 시 FastAPI `/ai/routine/adjust` 자동 호출 | **WeeklyReport (F304)**<br>- `POST /ai/report/weekly` — 7일 데이터 분석<br>- 체중 추세 numpy 선형 회귀<br>- Claude API 주간 피드백 생성 |
| **마지막 끼니 추천 (F802)**<br>- `POST /ai/meal/last-recommend` 호출 조건 판단<br>- 오늘 섭취 영양소 합계 → FastAPI 전달<br>- 추천 결과 수신 → Vue 홈 화면 카드 노출 | **주간 AI 조정**<br>- `POST /ai/plan/weekly-adjust`<br>- 칼로리 안전 하한선 적용 (여성 1200 / 남성 1500) |
| **Nyam (G101·G102·G104)**<br>- 달성률별 Nyam 외형 상태값 반환<br>- HealthGoal별 외형 변화<br>- 상황별 반응 메시지 enum 설계 |  |

### 2주차 산출물

| 구분 | 산출물 |
|---|---|
| API | `POST /routines`, `GET /routines/current`, `POST /routines/sessions` |
| API | `POST /ai/routine/generate`, `POST /ai/routine/adjust` (FastAPI) |
| API | `POST /ai/meal/last-recommend` (FastAPI) — 핵심 신규 |
| API | `POST /ai/report/weekly`, `POST /ai/plan/weekly-adjust` (FastAPI) |
| Vue | 홈 화면 “마지막 끼니 추천” 카드 컴포넌트 |

### 2주차 완료 기준

- 잔여칼로리 조건 충족 시 FastAPI 추천 3개 JSON 반환 확인
- 루틴 있는 사용자: 세션 기록 후 AI 조정 결과(UP/HOLD/DOWN/DELOAD) 반환
- WeeklyReport 호출 시 Claude 코멘트 포함 JSON 반환

---

## 3주차 2차 MVP — 커뮤니티 · 대시보드 · AI 코칭

**범위**: 팔로우 · 게시판 · 댓글 · 식단 피드 · AI 식단·운동 분석 · 뱃지

**목표**: 소셜 기능 + AI 코칭 확장. 뱃지가 레벨·코인을 대체하는 성장 지표로 작동

| Spring Boot (Java) | FastAPI (Python / AI) |
|---|---|
| **커뮤니티 (F401~F405)**<br>- F401 팔로우·팔로잉 CRUD<br>- F402 게시판 CRUD (식단 리뷰·전문가·자유)<br>- F403 댓글 CRUD<br>- F404 식단 피드 공유 (좋아요·댓글)<br>- F405 식단 템플릿 공유 | **AI 식단 분석 (F701)**<br>- `POST /ai/diet/analyze` — 영양 균형 분석<br>- 부족 영양소 감지 → 구체적 식품 추천<br>- Claude API 개선 방안 코멘트 |
| **대시보드 (F303)**<br>- 칼로리 밸런스·체중 추세·영양소 레이더 통합 API<br>- 이번 주 달성률·루틴 진행 현황<br>- 2주 AI 체크인 트리거 (F804) | **AI 운동 코칭 (F702)**<br>- `POST /ai/exercise/coach` — 루틴 기반 맞춤 주간 계획<br>- Claude API 운동 코멘트 생성 |
| **뱃지 확장 (G601~G603)**<br>- 레벨·코인 삭제 → 뱃지 조건 전면 확장<br>- 식단·운동·Streak·루틴 달성 조건 뱃지 설계<br>- G602 뱃지 컬렉션 조회 API<br>- G603 뱃지 획득 알림 이벤트 | **2주 AI 체크인 (F804)**<br>- `POST /ai/checkin/biweekly` — 달성률 분석<br>- 달성률 50% 미만 자동 트리거<br>- 목표 유지 / 조정 옵션 반환 |
| **Nyam 성장 (G101~G103)**<br>- G103 Streak·뱃지 달성 시 스킨 해금<br>- Nyam 상태 종합 API 완성 |  |
| **체중 기록 (F302)**<br>- 체중 날짜별 CRUD<br>- 체중 변화 추세 집계 |  |

### 3주차 산출물

| 구분 | 산출물 |
|---|---|
| API | `POST·GET /follows`, `GET /feeds`, `POST /feeds/{id}/like` |
| API | `POST·GET·PUT·DELETE /posts`, `POST·GET /comments` |
| API | `GET /dashboard/integrated`, `POST /weights`, `GET /badges` |
| API | `POST /ai/diet/analyze`, `POST /ai/exercise/coach` (FastAPI) |
| API | `POST /ai/checkin/biweekly` (FastAPI) |

---

## 4주차 3차 MVP — AI 고도화 · Nyam 완성 · 통합 QA

**범위**: RAG 채팅 · Multi-Agent · 개인화 미션 · 전체 통합 테스트

**목표**: 심화 AI 기능 + Nyam 성장 완성 + 전체 통합 테스트. 데모 준비

| Spring Boot (Java) | FastAPI (Python / AI) |
|---|---|
| **Nyam 완성 (G101~G104)**<br>- Nyam 외형 4단계 (달성률 0~30/30~60/60~90/90~100%)<br>- HealthGoal별 체형 변화 최종 구현<br>- G201 Streak 마일스톤 보상 (3·7·30·100일)<br>- G104 반응 메시지 전체 케이스 완성 | **RAG 영양 채팅 (F703)**<br>- 식품안전처 DB 벡터 DB 구축 (pgvector)<br>- `POST /ai/chat` — RAG 기반 영양 Q&A;<br>- 출처 명시 응답 구조 |
| **일일 미션 (G401·G402)**<br>- G401 일일 미션 3가지 자동 생성<br>- G402 전체 완료 보너스 (Coin 없음 — 뱃지·XP만)<br>- G403 개인화 미션 FastAPI 연동 | **Multi-Agent (F704)**<br>- 영양 분석 Agent<br>- 운동 루틴 추천 Agent<br>- 3 Agent 오케스트레이션 (Tool Calling) |
| **누적 통계 (G503)**<br>- 월별·전체 기간 식단·운동·체중 통계<br>- 루틴 무게 성장 히스토리 그래프 | **개인화 미션 (G403)**<br>- `POST /ai/mission/generate` — 목표·패턴 기반 3개 미션<br>- 단백질 부족 → “닭가슴살 먹기” 등 구체적 미션 |
| **스마트 알림 (F705)**<br>- Streak 위기 알림 (FCM)<br>- 루틴 미기록 알림<br>- DailyGoal 미달성 저녁 알림 | **스마트 알림 (F705)**<br>- `POST /ai/notification/timing` — 식습관·루틴 패턴<br>- 개인화 알림 타이밍 생성 |
| **통합 QA**<br>- 필수 기능 전체 E2E 테스트<br>- Spring ↔ FastAPI 연동 7개 포인트 전수 확인<br>- 마지막 끼니 추천 트리거 조건 E2E 확인<br>- 데모 시나리오 5개 준비 | **통합 테스트**<br>- FastAPI 전체 pytest<br>- 루틴 조정 단위 테스트 (UP/HOLD/DOWN/DELOAD)<br>- 마지막 끼니 추천 영양소 계산 검증 |

### 4주차 최종 DoD

- 필수 기능 전체 API 동작 확인
- 마지막 끼니 추천: 조건 충족 시 3개 식단 + 영양소 수치 반환
- 루틴 있는 사용자: 기존 루틴 → 4주치 점진 계획 → 2주 기록 → 자동 조정 E2E
- RAG 채팅: 식품안전처 DB 기반 질문 3개 이상 정상 응답
- `docker compose up` 한 번으로 전체 실행·데모 가능

---

## 전체 기능 담당 매핑 v3

코인 시스템 삭제, F802 마지막 끼니 추천으로 변경 반영

### 필수 기능

| 번호 | 기능명 | 담당 | 우선순위 |
|---|---|---|---|
| F101 | Meal 작성 | Spring Boot | 필수 |
| F102 | Meal 조회 | Spring Boot | 필수 |
| F103 | Meal 수정 | Spring Boot | 필수 |
| F104 | Meal 삭제 | Spring Boot | 필수 |
| F105 | 식단 분석 (영양소 합산) | Spring Boot | 필수 |
| F201 | 회원가입 | Spring Boot | 필수 |
| F202 | 회원 조회 | Spring Boot | 필수 |
| F203 | 회원 수정 | Spring Boot | 필수 |
| F204 | 회원 비활성화 | Spring Boot | 필수 |
| F205 | 로그인·로그아웃 | Spring Boot | 필수 |
| F206 | 온보딩 + AI 플랜 생성 | Spring Boot + FastAPI | 필수 |
| F302 | 체중 기록 CRUD | Spring Boot | 필수 |
| F801 | 칼로리 밸런스 연동 | Spring Boot | 필수 |
| F802 | 마지막 끼니 추천 (변경) | Spring Boot + FastAPI | 필수 |
| F901 | 운동 루틴 등록 | Spring Boot + FastAPI | 필수 |
| F902 | 주차별 루틴 생성 (AI) | FastAPI | 필수 |
| F903 | 루틴 성공/실패 기록 | Spring Boot | 필수 |
| F904 | 루틴 유기적 조정 (AI) | FastAPI | 필수 |
| G101 | Nyam 달성률별 외형 | Spring Boot | 필수 |
| G102 | HealthGoal별 Nyam 외형 | Spring Boot | 필수 |
| G104 | Nyam 반응 메시지 | Spring Boot | 필수 |
| G201 | Streak + 마일스톤 | Spring Boot | 필수 |
| G203 | Streak 알림 | Spring Boot | 필수 |
| G401 | 일일 미션 제공 | Spring Boot | 필수 |
| G402 | 미션 전체 완료 보너스 | Spring Boot | 필수 |
| G501 | 주간 달성 캘린더 | Spring Boot | 필수 |
| G502 | 목표 달성 진행바 | Spring Boot | 필수 |

### 추가 기능

| 번호 | 기능명 | 담당 | 우선순위 |
|---|---|---|---|
| F303 | 통합 대시보드 | Spring Boot | 추가 |
| F304 | 주간 AI 리포트 | FastAPI | 추가 |
| F401 | 팔로우·팔로잉 | Spring Boot | 추가 |
| F402 | 게시판 CRUD | Spring Boot | 추가 |
| F403 | 댓글 CRUD | Spring Boot | 추가 |
| F404 | 식단 피드 공유 | Spring Boot | 추가 |
| F405 | 식단 템플릿 공유 | Spring Boot | 추가 |
| F804 | 2주 AI 체크인 | FastAPI | 추가 |
| G103 | 캐릭터 스킨 해금 | Spring Boot | 추가 |
| G202 | Streak 보호 아이템 | Spring Boot | 추가 |
| G503 | 누적 기록 통계 | Spring Boot | 추가 |
| G601 | 뱃지 획득 (조건 확장) | Spring Boot | 추가 |
| G602 | 뱃지 컬렉션 조회 | Spring Boot | 추가 |
| G603 | 뱃지 획득 알림 | Spring Boot | 추가 |

### 심화 기능

| 번호 | 기능명 | 담당 | 우선순위 |
|---|---|---|---|
| F106 | Vision 식단 등록 | FastAPI | 심화 |
| F701 | AI 식단 분석 | FastAPI | 심화 |
| F702 | AI 운동 코칭 | FastAPI | 심화 |
| F703 | RAG 영양 채팅 | FastAPI | 심화 |
| F704 | Multi-Agent 코칭 | FastAPI | 심화 |
| F705 | 스마트 알림 | FastAPI | 심화 |
| G403 | 개인화 미션 생성 | FastAPI | 심화 |
