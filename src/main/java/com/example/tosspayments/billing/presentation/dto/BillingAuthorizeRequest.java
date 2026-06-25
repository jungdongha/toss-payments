package com.example.tosspayments.billing.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BillingAuthorizeRequest {

    @NotBlank(message = "authKey는 필수입니다.")
    private String authKey;      // 카드 등록 위젯에서 발급된 인증 코드

    @NotBlank(message = "customerKey는 필수입니다.")
    private String customerKey;  // 구매자 고유키 (서비스에서 관리)
}
