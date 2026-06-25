package com.example.tosspayments.order.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderPrepareRequest {

    @NotBlank(message = "orderId는 필수입니다.")
    private String orderId;         // 클라이언트에서 UUID로 생성

    @NotBlank(message = "orderName은 필수입니다.")
    private String orderName;

    @NotNull(message = "amount는 필수입니다.")
    @Positive(message = "amount는 0보다 커야 합니다.")
    private Long amount;            // 최종 결제 금액 (쿠폰/적립금 적용 후)

    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String customerEmail;

    private String customerName;
}
