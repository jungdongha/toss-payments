package com.example.tosspayments.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentCancelRequest {

    @NotBlank(message = "cancelReason은 필수입니다.")
    private String cancelReason;

    @Positive(message = "cancelAmount는 0보다 커야 합니다.")
    private Long cancelAmount;  // null이면 전액 취소
}
