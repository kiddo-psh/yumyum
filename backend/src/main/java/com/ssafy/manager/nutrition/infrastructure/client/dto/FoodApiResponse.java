package com.ssafy.manager.nutrition.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodApiResponse {

    private Body body;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private List<Item> items;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("FOOD_CD")      private String foodCode;
        @JsonProperty("FOOD_NM_KR")   private String foodName;
        @JsonProperty("SERVING_SIZE") private String servingSize;
        @JsonProperty("AMT_NUM1")     private String calories;
        @JsonProperty("AMT_NUM6")     private String carbs;
        @JsonProperty("AMT_NUM3")     private String protein;
        @JsonProperty("AMT_NUM4")     private String fat;
        @JsonProperty("AMT_NUM8")     private String fiber;
    }
}
