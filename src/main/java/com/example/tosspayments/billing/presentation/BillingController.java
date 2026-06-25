package com.example.tosspayments.billing.presentation;

import com.example.tosspayments.billing.application.BillingService;
import com.example.tosspayments.billing.presentation.dto.BillingAuthorizeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingChargeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingResponse;
import com.example.tosspayments.payment.presentation.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /** 빌링키 발급 */
    @PostMapping("/authorize")
    public ResponseEntity<BillingResponse> authorize(
            @Valid @RequestBody BillingAuthorizeRequest request) {
        return ResponseEntity.ok(billingService.issueBillingKey(request));
    }

    /** 자동결제 실행 */
    @PostMapping("/{billingKey}/charge")
    public ResponseEntity<PaymentResponse> charge(
            @PathVariable String billingKey,
            @Valid @RequestBody BillingChargeRequest request) {
        return ResponseEntity.ok(billingService.charge(billingKey, request));
    }
}
