package com.example.tosspayments.payment.infrastructure;

import com.example.tosspayments.billing.presentation.dto.BillingAuthorizeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingChargeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingResponse;
import com.example.tosspayments.global.client.TossApiErrorResponse;
import com.example.tosspayments.global.exception.TossPaymentsException;
import com.example.tosspayments.payment.presentation.dto.PaymentCancelRequest;
import com.example.tosspayments.payment.presentation.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 토스페이먼츠 외부 API HTTP 클라이언트 (Infrastructure Layer)
 *
 * Application Service는 이 클라이언트를 통해서만 토스 API를 호출합니다.
 * 모든 HTTP 통신과 에러 매핑을 이 클래스가 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentsClient {

    private final WebClient tossWebClient;

    // ── 결제 ────────────────────────────────────

    /**
     * 결제 승인 API
     * POST /v1/payments/confirm
     */
    public PaymentResponse confirm(String paymentKey, String orderId, Long amount) {
        log.info("[Toss] 결제 승인 요청 — orderId: {}, amount: {}", orderId, amount);
        return tossWebClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(Map.of(
                        "paymentKey", paymentKey,
                        "orderId",    orderId,
                        "amount",     amount
                ))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(TossApiErrorResponse.class)
                                .flatMap(err -> Mono.error(new TossPaymentsException(err.getCode(), err.getMessage()))))
                .bodyToMono(PaymentResponse.class)
                .block();
    }

    /**
     * 결제 취소 API
     * POST /v1/payments/{paymentKey}/cancel
     */
    public PaymentResponse cancel(String paymentKey, PaymentCancelRequest request) {
        log.info("[Toss] 결제 취소 요청 — paymentKey: {}", paymentKey);
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", request.getCancelReason());
        if (request.getCancelAmount() != null) {
            body.put("cancelAmount", request.getCancelAmount());
        }

        return tossWebClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(TossApiErrorResponse.class)
                                .flatMap(err -> Mono.error(new TossPaymentsException(err.getCode(), err.getMessage()))))
                .bodyToMono(PaymentResponse.class)
                .block();
    }

    /**
     * paymentKey로 결제 단건 조회
     * GET /v1/payments/{paymentKey}
     */
    public PaymentResponse getByPaymentKey(String paymentKey) {
        return tossWebClient.get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(TossApiErrorResponse.class)
                                .flatMap(err -> Mono.error(new TossPaymentsException(err.getCode(), err.getMessage()))))
                .bodyToMono(PaymentResponse.class)
                .block();
    }

    /**
     * orderId로 결제 조회
     * GET /v1/payments/orders/{orderId}
     */
    public PaymentResponse getByOrderId(String orderId) {
        return tossWebClient.get()
                .uri("/v1/payments/orders/{orderId}", orderId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(TossApiErrorResponse.class)
                                .flatMap(err -> Mono.error(new TossPaymentsException(err.getCode(), err.getMessage()))))
                .bodyToMono(PaymentResponse.class)
                .block();
    }

    // ── 자동결제 (Billing) ──────────────────────

    /**
     * 빌링키 발급
     * POST /v1/billing/authorizations/issue
     */
    public BillingResponse issueBillingKey(BillingAuthorizeRequest request) {
        log.info("[Toss] 빌링키 발급 요청 — customerKey: {}", request.getCustomerKey());
        return tossWebClient.post()
                .uri("/v1/billing/authorizations/issue")
                .bodyValue(Map.of(
                        "authKey",     request.getAuthKey(),
                        "customerKey", request.getCustomerKey()
                ))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(TossApiErrorResponse.class)
                                .flatMap(err -> Mono.error(new TossPaymentsException(err.getCode(), err.getMessage()))))
                .bodyToMono(BillingResponse.class)
                .block();
    }

    /**
     * 자동결제 실행
     * POST /v1/billing/{billingKey}
     */
    public PaymentResponse chargeBilling(String billingKey, BillingChargeRequest request) {
        log.info("[Toss] 자동결제 요청 — billingKey: {}, orderId: {}", billingKey, request.getOrderId());
        return tossWebClient.post()
                .uri("/v1/billing/{billingKey}", billingKey)
                .bodyValue(Map.of(
                        "customerKey", request.getCustomerKey(),
                        "amount",      request.getAmount(),
                        "orderId",     request.getOrderId(),
                        "orderName",   request.getOrderName()
                ))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(TossApiErrorResponse.class)
                                .flatMap(err -> Mono.error(new TossPaymentsException(err.getCode(), err.getMessage()))))
                .bodyToMono(PaymentResponse.class)
                .block();
    }
}
