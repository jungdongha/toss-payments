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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock private TossPaymentsClient tossClient;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    private Order sampleOrder;
    private PaymentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .orderId("order-uuid-1")
                .orderName("상품명")
                .amount(15_000L)
                .customerEmail("buyer@example.com")
                .customerName("구매자")
                .build();

        sampleResponse = new PaymentResponse();
        ReflectionTestUtils.setField(sampleResponse, "paymentKey", "toss_pk_001");
        ReflectionTestUtils.setField(sampleResponse, "orderId", "order-uuid-1");
        ReflectionTestUtils.setField(sampleResponse, "orderName", "상품명");
        ReflectionTestUtils.setField(sampleResponse, "totalAmount", 15_000L);
        ReflectionTestUtils.setField(sampleResponse, "status", "DONE");
        ReflectionTestUtils.setField(sampleResponse, "method", "카드");
        ReflectionTestUtils.setField(sampleResponse, "secret", "webhook-secret-xyz");
    }

    // ── confirm ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 승인 — 금액 검증 통과 후 Payment 저장")
    void confirm_success_savesPayment() {
        // given
        PaymentConfirmRequest request = makeConfirmRequest("toss_pk_001", "order-uuid-1", 15_000L);

        given(orderService.verifyAmount("order-uuid-1", 15_000L)).willReturn(sampleOrder);
        given(tossClient.confirm("toss_pk_001", "order-uuid-1", 15_000L)).willReturn(sampleResponse);
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        PaymentResponse result = paymentService.confirm(request);

        // then
        assertThat(result.getPaymentKey()).isEqualTo("toss_pk_001");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        then(paymentRepository).should().save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getPaymentKey()).isEqualTo("toss_pk_001");
        assertThat(saved.getAmount()).isEqualTo(15_000L);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(saved.getSecret()).isEqualTo("webhook-secret-xyz");  // secret 저장 검증
    }

    @Test
    @DisplayName("결제 승인 — 금액 검증은 OrderService에 위임")
    void confirm_delegatesAmountVerificationToOrderService() {
        // given
        PaymentConfirmRequest request = makeConfirmRequest("toss_pk_001", "order-uuid-1", 15_000L);

        given(orderService.verifyAmount("order-uuid-1", 15_000L)).willReturn(sampleOrder);
        given(tossClient.confirm(any(), any(), any())).willReturn(sampleResponse);
        given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        paymentService.confirm(request);

        // then
        then(orderService).should(times(1)).verifyAmount("order-uuid-1", 15_000L);
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 전액 취소 — Payment 상태 CANCELED 전환")
    void cancel_fullCancel_updatesStatusToCanceled() {
        // given
        Payment existingPayment = Payment.builder()
                .paymentKey("toss_pk_001")
                .orderId("order-uuid-1")
                .orderName("상품명")
                .amount(15_000L)
                .status(PaymentStatus.DONE)
                .method("카드")
                .build();

        PaymentResponse cancelResponse = new PaymentResponse();
        ReflectionTestUtils.setField(cancelResponse, "paymentKey", "toss_pk_001");
        ReflectionTestUtils.setField(cancelResponse, "status", "CANCELED");

        PaymentCancelRequest cancelRequest = new PaymentCancelRequest();
        ReflectionTestUtils.setField(cancelRequest, "cancelReason", "단순 변심");

        given(tossClient.cancel("toss_pk_001", cancelRequest)).willReturn(cancelResponse);
        given(paymentRepository.findByPaymentKey("toss_pk_001")).willReturn(Optional.of(existingPayment));
        given(orderService.getByOrderId("order-uuid-1")).willReturn(sampleOrder);

        // when
        PaymentResponse result = paymentService.cancel("toss_pk_001", cancelRequest);

        // then
        assertThat(result.getStatus()).isEqualTo("CANCELED");
        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    @DisplayName("결제 부분 취소 — Payment 상태 PARTIAL_CANCELED 전환")
    void cancel_partialCancel_updatesStatusToPartialCanceled() {
        // given
        Payment existingPayment = Payment.builder()
                .paymentKey("toss_pk_001")
                .orderId("order-uuid-1")
                .orderName("상품명")
                .amount(15_000L)
                .status(PaymentStatus.DONE)
                .method("카드")
                .build();

        PaymentResponse cancelResponse = new PaymentResponse();
        ReflectionTestUtils.setField(cancelResponse, "paymentKey", "toss_pk_001");
        ReflectionTestUtils.setField(cancelResponse, "status", "PARTIAL_CANCELED");

        PaymentCancelRequest cancelRequest = new PaymentCancelRequest();
        ReflectionTestUtils.setField(cancelRequest, "cancelReason", "일부 반품");
        ReflectionTestUtils.setField(cancelRequest, "cancelAmount", 5_000L);

        given(tossClient.cancel("toss_pk_001", cancelRequest)).willReturn(cancelResponse);
        given(paymentRepository.findByPaymentKey("toss_pk_001")).willReturn(Optional.of(existingPayment));
        given(orderService.getByOrderId("order-uuid-1")).willReturn(sampleOrder);

        // when
        paymentService.cancel("toss_pk_001", cancelRequest);

        // then
        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private PaymentConfirmRequest makeConfirmRequest(String paymentKey, String orderId, Long amount) {
        PaymentConfirmRequest req = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(req, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(req, "orderId", orderId);
        ReflectionTestUtils.setField(req, "amount", amount);
        return req;
    }
}
