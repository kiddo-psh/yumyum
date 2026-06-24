package com.ssafy.manager.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mfds.api.food")
public record FoodApiProperties(
        String baseUrl,
        String serviceKey,
        String responseType,
        int pageSize,
        Duration duration
) {}
