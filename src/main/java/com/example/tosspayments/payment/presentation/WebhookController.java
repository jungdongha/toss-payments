package com.example.tosspayments.payment.presentation;

import com.example.tosspayments.order.domain.OrderRepository;
import com.example.tosspayments.payment.domain.PaymentRepository;
import com.example.tosspayments.payment.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 토스페이먼츠 웹훅 수신
 *
 * 주요 이벤트:
 *   - DEPOSIT_CALLBACK : 가상계좌 입금 완료
 *   - 빌링 상태 변경, 지급대행 상태 변경
 *
 * 개발자센터 등록: https://developers.tosspayments.com/my/webhook
 *
 * [응답 규칙]
 * 토스 서버는 200 외 응답 시 최대 5회 재시도하므로, 처리 성공 여부와 관계없이
 * 빠르게 200을 반환하고 실제 처리는 내부에서 수행합니다.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;

    @PostMapping("/toss")
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody Map<String, String> payload) {
        String orderId = payload.get("orderId");
        String status  = payload.get("status");
        String secret  = payload.get("secret");

        log.info("[Webhook] 수신 — orderId: {}, status: {}", orderId, status);

        if (orderId == null || status == null) {
            log.warn("[Webhook] 필수 파라미터 누락 payload: {}", payload);
            return ResponseEntity.ok().build(); // 200 우선 반환
        }

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            // secret 검증 — 토스 서버 발송 여부 확인
            if (payment.getSecret() != null && !payment.getSecret().equals(secret)) {
                log.warn("[Webhook] secret 불일치 — orderId: {} (위조 요청 의심)", orderId);
                return;
            }

            if ("DONE".equals(status)) {
                payment.updateStatus(PaymentStatus.DONE);
                orderRepository.findByOrderId(orderId)
                        .ifPresent(order -> {
                            order.markAsPaid();
                            log.info("[Webhook] 가상계좌 입금 완료 — orderId: {}", orderId);
                        });
            } else if ("CANCELED".equals(status)) {
                payment.updateStatus(PaymentStatus.CANCELED);
                orderRepository.findByOrderId(orderId)
                        .ifPresent(order -> order.markAsCanceled());
            }
        });

        return ResponseEntity.ok().build();
    }
}
