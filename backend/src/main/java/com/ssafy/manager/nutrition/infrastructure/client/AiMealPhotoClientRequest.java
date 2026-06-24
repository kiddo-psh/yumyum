package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiMealPhotoClientRequest(
        @JsonProperty("image_base64") String imageBase64,
        @JsonProperty("media_type")   String mediaType,
        @JsonProperty("meal_type")    String mealType
) {}
