package com.example.tosspayments.payment.presentation.dto;

import com.example.tosspayments.payment.domain.Payment;
import com.example.tosspayments.payment.domain.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DB 저장 결제 내역 목록 응답 DTO
 *
 * Payment JPA Entity를 직접 노출하지 않으며,
 * 가상계좌 웹훅 검증용 secret 필드를 의도적으로 제외합니다.
 */
@Getter
public class PaymentSummaryResponse {

    private final Long id;
    private final String paymentKey;
    private final String orderId;
    private final String orderName;
    private final Long amount;
    private final PaymentStatus status;
    private final String method;
    private final String customerEmail;
    private final String customerName;
    private final LocalDateTime approvedAt;
    private final LocalDateTime createdAt;

    private PaymentSummaryResponse(Payment payment) {
        this.id            = payment.getId();
        this.paymentKey    = payment.getPaymentKey();
        this.orderId       = payment.getOrderId();
        this.orderName     = payment.getOrderName();
        this.amount        = payment.getAmount();
        this.status        = payment.getStatus();
        this.method        = payment.getMethod();
        this.customerEmail = payment.getCustomerEmail();
        this.customerName  = payment.getCustomerName();
        this.approvedAt    = payment.getApprovedAt();
        this.createdAt     = payment.getCreatedAt();
    }

    public static PaymentSummaryResponse from(Payment payment) {
        return new PaymentSummaryResponse(payment);
    }
}
