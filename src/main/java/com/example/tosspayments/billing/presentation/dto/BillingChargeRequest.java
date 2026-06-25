package com.example.tosspayments.billing.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BillingChargeRequest {

    @NotBlank(message = "customerKey는 필수입니다.")
    private String customerKey;

    @NotNull(message = "amount는 필수입니다.")
    @Positive(message = "amount는 0보다 커야 합니다.")
    private Long amount;

    @NotBlank(message = "orderId는 필수입니다.")
    private String orderId;

    @NotBlank(message = "orderName은 필수입니다.")
    private String orderName;

    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String customerEmail;

    private String customerName;
}
