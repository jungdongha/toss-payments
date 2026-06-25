package com.example.tosspayments.global.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 토스페이먼츠 API 에러 응답 형식 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossApiErrorResponse {

    private String code;
    private String message;
}
