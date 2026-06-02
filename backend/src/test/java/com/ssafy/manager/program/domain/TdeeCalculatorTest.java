package com.ssafy.manager.program.domain;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.Sex;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Mifflin-St Jeor 공식
// 남성 BMR = 10×체중 + 6.25×키 - 5×나이 + 5
// 여성 BMR = 10×체중 + 6.25×키 - 5×나이 - 161
// TDEE = BMR × 활동량 승수 (반올림)
class TdeeCalculatorTest {

    // 남성, 36세, 175cm, 80kg, 보통활동 → BMR=1718.75, TDEE=2664.06 → 2664
    @Test
    void 남성_TDEE를_Mifflin_St_Jeor_공식으로_계산한다() {
        int tdee = TdeeCalculator.calculate(Sex.MALE, 36, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE);

        assertThat(tdee).isEqualTo(2664);
    }

    // 여성, 36세, 175cm, 80kg, 보통활동 → BMR=1552.75, TDEE=2406.76 → 2407
    @Test
    void 여성은_남성보다_BMR이_낮게_계산된다() {
        int male = TdeeCalculator.calculate(Sex.MALE, 36, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE);
        int female = TdeeCalculator.calculate(Sex.FEMALE, 36, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE);

        assertThat(female).isEqualTo(2407);
        assertThat(female).isLessThan(male);
    }

    // 남성, 36세, 175cm, 80kg → BMR=1718.75
    // SEDENTARY(×1.2)=2062.5→2063, VERY_ACTIVE(×1.725)=2964.84→2965
    @Test
    void 활동량이_높을수록_TDEE가_높게_계산된다() {
        int sedentary = TdeeCalculator.calculate(Sex.MALE, 36, 175.0, 80.0, ActivityLevel.SEDENTARY);
        int veryActive = TdeeCalculator.calculate(Sex.MALE, 36, 175.0, 80.0, ActivityLevel.VERY_ACTIVE);

        assertThat(sedentary).isEqualTo(2063);
        assertThat(veryActive).isEqualTo(2965);
        assertThat(sedentary).isLessThan(veryActive);
    }
}
