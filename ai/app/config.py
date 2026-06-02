from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # GMS API
    gms_api_key: str = "mock-key"
    gms_base_url: str = "https://gms.ssafy.io/gmsapi/api.anthropic.com"
    anthropic_version: str = "2023-06-01"

    # 모델 설정
    # 기본: haiku (빠름·저비용 — 식단추천·코멘트 생성용)
    # 심화: claude-opus-4-5-20251101 (RAG·Multi-Agent 4주차용)
    default_model: str = "claude-haiku-4-5-20251001"

    # 식품안전처 공공데이터 API (data.go.kr)
    mfds_api_key: str = "mock-key"

    spring_base_url: str = "http://backend:8080"
    env: str = "dev"

    model_config = SettingsConfigDict(env_file=".env")


settings = Settings()
