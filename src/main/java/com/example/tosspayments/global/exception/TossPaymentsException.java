package com.example.tosspayments.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TossPaymentsException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    /** 토스 API 에러 응답 기반 생성 */
    public TossPaymentsException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    /** HTTP 상태 코드 지정 생성 */
    public TossPaymentsException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
