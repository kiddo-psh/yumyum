from fastapi import APIRouter
from app.schemas.report import (
    WeeklyReportRequest, WeeklyReportResponse,
    WeeklyAdjustRequest, WeeklyAdjustResponse,
)
from app.services.claude_service import call_claude
from app.services.trend_service import calc_weight_trend, calc_calorie_adjustment

router = APIRouter(tags=["AI Report"])


@router.post("/ai/report/weekly", response_model=WeeklyReportResponse)
async def weekly_report(req: WeeklyReportRequest):
    """
    F304 - 주간 리포트 생성
    7일 식단·운동 데이터를 분석해 달성률과 체중 추세를 계산하고 AI 피드백을 반환한다.
    """
    def rate(actual: float, target: float) -> float:
        return round(actual / target * 100, 1) if target > 0 else 100.0

    calorie_rates = [
        rate(d.kcal + d.calories_burned, req.target_kcal)
        for d in req.daily_nutrition
    ]
    protein_rates  = [rate(d.protein_g, req.target_protein_g) for d in req.daily_nutrition]
    carb_rates     = [rate(d.carb_g,    req.target_carb_g)    for d in req.daily_nutrition]
    fat_rates      = [rate(d.fat_g,     req.target_fat_g)     for d in req.daily_nutrition]

    avg_cal  = round(sum(calorie_rates) / len(calorie_rates), 1) if calorie_rates else 0.0
    avg_pro  = round(sum(protein_rates) / len(protein_rates), 1) if protein_rates else 0.0
    avg_carb = round(sum(carb_rates)    / len(carb_rates),    1) if carb_rates    else 0.0
    avg_fat  = round(sum(fat_rates)     / len(fat_rates),     1) if fat_rates     else 0.0

    achievement_days = sum(1 for r in calorie_rates if 80 <= r <= 120)

    weights = [w.weight_kg for w in req.weight_records]
    weight_trend = calc_weight_trend(weights)

    trend_text = (
        f"체중 추세: {weight_trend:+.2f}kg/주"
        if weight_trend is not None
        else "이번 주 체중 기록 없음"
    )
    prompt = (
        f"[{req.week_number}주차 리포트] 건강 목표: {req.health_goal}\n"
        f"칼로리 달성률 평균: {avg_cal:.0f}% | 단백질: {avg_pro:.0f}% | "
        f"탄수화물: {avg_carb:.0f}% | 지방: {avg_fat:.0f}%\n"
        f"목표 달성일: {achievement_days}일 / {len(req.daily_nutrition)}일\n"
        f"{trend_text}\n"
        "위 결과를 바탕으로 이번 주를 격려하고 다음 주 개선 방향을 2~3문장으로 한국어로 제안하세요."
    )

    ai_comment = await call_claude(prompt, max_tokens=400)

    return WeeklyReportResponse(
        avg_calorie_rate=avg_cal,
        avg_protein_rate=avg_pro,
        avg_carb_rate=avg_carb,
        avg_fat_rate=avg_fat,
        achievement_days=achievement_days,
        weight_trend=weight_trend,
        ai_comment=ai_comment,
    )


@router.post("/ai/plan/weekly-adjust", response_model=WeeklyAdjustResponse)
async def weekly_adjust(req: WeeklyAdjustRequest):
    """
    연동 #6 - 주간 체중 추세 기반 목표 칼로리 재조정
    /ai/report/weekly 응답의 weight_trend를 받아 다음 주 칼로리 목표를 조정한다.
    """
    new_kcal, reason = calc_calorie_adjustment(
        req.current_target_kcal, req.health_goal, req.weight_trend, req.sex
    )
    adjustment = new_kcal - req.current_target_kcal

    prompt = (
        f"[{req.week_number}주차 칼로리 조정] 목표: {req.health_goal}\n"
        f"기존: {req.current_target_kcal:.0f}kcal → 조정: {new_kcal:.0f}kcal ({adjustment:+.0f})\n"
        f"이유: {reason}\n"
        "이 조정 내용을 사용자에게 이해하기 쉽게 2문장으로 한국어로 설명하세요."
    )

    ai_comment = await call_claude(prompt, max_tokens=200)

    return WeeklyAdjustResponse(
        new_target_kcal=new_kcal,
        adjustment=adjustment,
        reason=reason,
        ai_comment=ai_comment,
    )
