package com.ssafy.manager.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FoodApiProperties.class)
public class RestClientConfig {

    private SimpleClientHttpRequestFactory fastapiRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);   // 3 seconds
        factory.setReadTimeout(15000);     // 15 seconds (AI calls can be slow)
        return factory;
    }

    @Bean
    RestClient aiPlanRestClient(
            @Value("${ai.fastapi.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fastapiRequestFactory())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    RestClient aiRoutineRestClient(
            @Value("${ai.fastapi.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fastapiRequestFactory())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    RestClient aiMealRestClient(
            @Value("${ai.fastapi.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fastapiRequestFactory())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    RestClient foodApiRestClient(FoodApiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(properties.duration());
        factory.setConnectTimeout(properties.duration());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
