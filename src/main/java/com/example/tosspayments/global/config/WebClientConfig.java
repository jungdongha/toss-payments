package com.example.tosspayments.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

/**
 * 토스페이먼츠 WebClient 설정
 *
 * Basic 인증: base64("{SECRET_KEY}:")
 * 시크릿 키 뒤에 반드시 콜론(:)을 붙여야 합니다.
 */
@Configuration
public class WebClientConfig {

    private final TossPaymentsProperties properties;

    public WebClientConfig(TossPaymentsProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WebClient tossWebClient() {
        String encoded = Base64.getEncoder()
                .encodeToString((properties.getSecretKey() + ":").getBytes());

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
