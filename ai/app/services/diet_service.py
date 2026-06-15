from typing import Dict, List, Tuple


def calculate_diet_analysis(
    total_kcal: float, total_protein_g: float,
    total_carb_g: float, total_fat_g: float,
    target_kcal: float, target_protein_g: float,
    target_carb_g: float, target_fat_g: float,
) -> Tuple[Dict[str, float], float, List[str], List[str]]:
    """
    Returns:
        rates: {"calorie_rate": float, "protein_rate": float, "carb_rate": float, "fat_rate": float}
        balance_score: 0~100 (100 = 완벽한 균형)
        weak_nutrients: 달성률 80% 미만 항목 리스트
        excess_nutrients: 달성률 120% 초과 항목 리스트
    """
    def rate(actual: float, target: float) -> float:
        if target <= 0:
            return 100.0
        return round(actual / target * 100, 2)

    rates = {
        "calorie_rate": rate(total_kcal, target_kcal),
        "protein_rate": rate(total_protein_g, target_protein_g),
        "carb_rate":    rate(total_carb_g, target_carb_g),
        "fat_rate":     rate(total_fat_g, target_fat_g),
    }

    values = list(rates.values())
    mean_deviation = sum(abs(r - 100) for r in values) / len(values)
    balance_score = round(max(0.0, 100.0 - mean_deviation), 2)

    label_map = {
        "calorie_rate": "calorie",
        "protein_rate": "protein",
        "carb_rate":    "carb",
        "fat_rate":     "fat",
    }
    weak   = [label_map[k] for k, v in rates.items() if v < 80]
    excess = [label_map[k] for k, v in rates.items() if v > 120]

    return rates, balance_score, weak, excess
