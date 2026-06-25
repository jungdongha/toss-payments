package com.example.tosspayments.payment.application;

import com.example.tosspayments.order.application.OrderService;
import com.example.tosspayments.order.domain.Order;
import com.example.tosspayments.payment.domain.Payment;
import com.example.tosspayments.payment.domain.PaymentRepository;
import com.example.tosspayments.payment.domain.PaymentStatus;
import com.example.tosspayments.payment.infrastructure.TossPaymentsClient;
import com.example.tosspayments.payment.presentation.dto.PaymentCancelRequest;
import com.example.tosspayments.payment.presentation.dto.PaymentConfirmRequest;
import com.example.tosspayments.payment.presentation.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 결제 Application Service
 *
 * 비즈니스 유스케이스를 조율합니다.
 * - 외부 API 호출 → TossPaymentsClient (Infrastructure)
 * - 주문 금액 검증 → OrderService
 * - 결제 영속화 → PaymentRepository (Domain)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final TossPaymentsClient tossClient;
    private final PaymentRepository  paymentRepository;
    private final OrderService       orderService;

    /**
     * 결제 승인
     *
     * 1. DB 저장 금액 vs 클라이언트 요청 금액 검증 (위변조 방지)
     * 2. 토스 결제 승인 API 호출
     * 3. Payment 저장 + Order 상태 PAID 전환
     */
    @Transactional
    public PaymentResponse confirm(PaymentConfirmRequest request) {
        // Step 1 — 금액 검증
        Order order = orderService.verifyAmount(request.getOrderId(), request.getAmount());

        // Step 2 — 토스 API 승인
        PaymentResponse response = tossClient.confirm(
                request.getPaymentKey(),
                request.getOrderId(),
                request.getAmount()
        );

        // Step 3 — 영속화
        paymentRepository.save(Payment.builder()
                .paymentKey   (response.getPaymentKey())
                .orderId      (response.getOrderId())
                .orderName    (response.getOrderName())
                .amount       (response.getTotalAmount())
                .status       (PaymentStatus.DONE)
                .method       (response.getMethod())
                .customerEmail(order.getCustomerEmail())
                .customerName (order.getCustomerName())
                .secret       (response.getSecret())
                .approvedAt   (toLocalDateTime(response.getApprovedAt()))
                .build());

        order.markAsPaid();
        log.info("[Payment] 승인 완료 — paymentKey: {}", response.getPaymentKey());
        return response;
    }

    /**
     * 결제 취소 / 부분 취소
     * cancelAmount null → 전액 취소
     */
    @Transactional
    public PaymentResponse cancel(String paymentKey, PaymentCancelRequest request) {
        PaymentResponse response = tossClient.cancel(paymentKey, request);

        paymentRepository.findByPaymentKey(paymentKey).ifPresent(payment -> {
            PaymentStatus next = "PARTIAL_CANCELED".equals(response.getStatus())
                    ? PaymentStatus.PARTIAL_CANCELED
                    : PaymentStatus.CANCELED;
            payment.updateStatus(next);
            orderService.getByOrderId(payment.getOrderId()).markAsCanceled();
        });

        log.info("[Payment] 취소 완료 — paymentKey: {}, status: {}", paymentKey, response.getStatus());
        return response;
    }

    /** paymentKey로 단건 조회 (토스 API) */
    public PaymentResponse getByPaymentKey(String paymentKey) {
        return tossClient.getByPaymentKey(paymentKey);
    }

    /** orderId로 조회 (토스 API) */
    public PaymentResponse getByOrderId(String orderId) {
        return tossClient.getByOrderId(orderId);
    }

    /** DB 저장 결제 내역 전체 조회 */
    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }

    // ── Helpers ──────────────────────────────────

    private LocalDateTime toLocalDateTime(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }
}
