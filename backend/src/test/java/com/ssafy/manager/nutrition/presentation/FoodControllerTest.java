package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.infrastructure.persistence.FoodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FoodController.class)
class FoodControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FoodRepository foodRepository;

    @Test
    void query가_없으면_전체_목록을_반환한다() throws Exception {
        given(foodRepository.findByNameContaining("")).willReturn(List.of(
                new Food("닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0),
                new Food("현미밥", 100.0, 22.0, 2.5, 0.8, 1.5)
        ));

        mockMvc.perform(get("/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 키워드로_음식_목록을_반환한다() throws Exception {
        given(foodRepository.findByNameContaining("닭")).willReturn(List.of(
                new Food("닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0)
        ));

        mockMvc.perform(get("/foods").param("query", "닭"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("닭가슴살"))
                .andExpect(jsonPath("$[0].caloriesPer100g").value(165.0));
    }
}
