package com.example.tosspayments.billing.application;

import com.example.tosspayments.billing.presentation.dto.BillingAuthorizeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingChargeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingResponse;
import com.example.tosspayments.payment.domain.Payment;
import com.example.tosspayments.payment.domain.PaymentRepository;
import com.example.tosspayments.payment.domain.PaymentStatus;
import com.example.tosspayments.payment.infrastructure.TossPaymentsClient;
import com.example.tosspayments.payment.presentation.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 자동결제(빌링) Application Service
 *
 * 정기 구독, 반복 결제 등에 사용됩니다.
 * 빌링키 발급 → 자동결제 실행의 2단계 흐름입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final TossPaymentsClient tossClient;
    private final PaymentRepository  paymentRepository;

    /**
     * 빌링키 발급
     * 카드 등록 위젯 성공 후 authKey + customerKey를 받아 호출
     */
    public BillingResponse issueBillingKey(BillingAuthorizeRequest request) {
        BillingResponse response = tossClient.issueBillingKey(request);
        log.info("[Billing] 빌링키 발급 완료 — customerKey: {}", request.getCustomerKey());
        return response;
    }

    /**
     * 자동결제 실행
     * 발급된 billingKey로 정기 결제를 실행합니다.
     */
    @Transactional
    public PaymentResponse charge(String billingKey, BillingChargeRequest request) {
        PaymentResponse response = tossClient.chargeBilling(billingKey, request);

        paymentRepository.save(Payment.builder()
                .paymentKey   (response.getPaymentKey())
                .orderId      (response.getOrderId())
                .orderName    (response.getOrderName())
                .amount       (response.getTotalAmount())
                .status       (PaymentStatus.DONE)
                .method       (response.getMethod())
                .customerEmail(request.getCustomerEmail())
                .customerName (request.getCustomerName())
                .approvedAt   (toLocalDateTime(response.getApprovedAt()))
                .build());

        log.info("[Billing] 자동결제 완료 — paymentKey: {}, orderId: {}",
                response.getPaymentKey(), response.getOrderId());
        return response;
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }
}
