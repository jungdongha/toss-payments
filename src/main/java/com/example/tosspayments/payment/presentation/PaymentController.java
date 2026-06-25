package com.example.tosspayments.payment.presentation;

import com.example.tosspayments.payment.application.PaymentService;
import com.example.tosspayments.payment.presentation.dto.PaymentCancelRequest;
import com.example.tosspayments.payment.presentation.dto.PaymentConfirmRequest;
import com.example.tosspayments.payment.presentation.dto.PaymentResponse;
import com.example.tosspayments.payment.presentation.dto.PaymentSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** 결제 승인 */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirm(
            @Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirm(request));
    }

    /** 결제 취소 / 환불 */
    @PostMapping("/{paymentKey}/cancel")
    public ResponseEntity<PaymentResponse> cancel(
            @PathVariable String paymentKey,
            @Valid @RequestBody PaymentCancelRequest request) {
        return ResponseEntity.ok(paymentService.cancel(paymentKey, request));
    }

    /** 결제 단건 조회 */
    @GetMapping("/{paymentKey}")
    public ResponseEntity<PaymentResponse> getByPaymentKey(
            @PathVariable String paymentKey) {
        return ResponseEntity.ok(paymentService.getByPaymentKey(paymentKey));
    }

    /** 주문번호로 결제 조회 */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(
            @PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }

    /** DB 저장 결제 내역 목록 (secret 필드 제외) */
    @GetMapping
    public ResponseEntity<List<PaymentSummaryResponse>> getAll() {
        List<PaymentSummaryResponse> list = paymentService.getAll().stream()
                .map(PaymentSummaryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
