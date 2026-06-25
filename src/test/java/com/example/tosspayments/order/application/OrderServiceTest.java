package com.example.tosspayments.order.application;

import com.example.tosspayments.global.exception.TossPaymentsException;
import com.example.tosspayments.order.domain.Order;
import com.example.tosspayments.order.domain.OrderRepository;
import com.example.tosspayments.order.domain.OrderStatus;
import com.example.tosspayments.order.presentation.dto.OrderPrepareRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .orderId("order-uuid-1234")
                .orderName("테스트 상품")
                .amount(10_000L)
                .customerEmail("test@example.com")
                .customerName("홍길동")
                .build();
    }

    // ── prepareOrder ────────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 주문 임시 저장 — 신규 orderId면 저장 성공")
    void prepareOrder_success() {
        // given
        OrderPrepareRequest request = mockRequest("order-uuid-1234", "테스트 상품", 10_000L);
        given(orderRepository.findByOrderId("order-uuid-1234")).willReturn(Optional.empty());
        given(orderRepository.save(any(Order.class))).willReturn(sampleOrder);

        // when
        Order result = orderService.prepareOrder(request);

        // then
        assertThat(result.getOrderId()).isEqualTo("order-uuid-1234");
        assertThat(result.getAmount()).isEqualTo(10_000L);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    @DisplayName("중복 orderId — ALREADY_EXISTS_ORDER 예외 발생")
    void prepareOrder_duplicateOrderId_throwsConflict() {
        // given
        OrderPrepareRequest request = mockRequest("order-uuid-1234", "테스트 상품", 10_000L);
        given(orderRepository.findByOrderId("order-uuid-1234")).willReturn(Optional.of(sampleOrder));

        // when & then
        assertThatThrownBy(() -> orderService.prepareOrder(request))
                .isInstanceOf(TossPaymentsException.class)
                .satisfies(ex -> {
                    TossPaymentsException tpe = (TossPaymentsException) ex;
                    assertThat(tpe.getCode()).isEqualTo("ALREADY_EXISTS_ORDER");
                    assertThat(tpe.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    // ── verifyAmount ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("금액 일치 — Order 반환")
    void verifyAmount_match_returnsOrder() {
        // given
        given(orderRepository.findByOrderId("order-uuid-1234")).willReturn(Optional.of(sampleOrder));

        // when
        Order result = orderService.verifyAmount("order-uuid-1234", 10_000L);

        // then
        assertThat(result).isEqualTo(sampleOrder);
    }

    @Test
    @DisplayName("금액 불일치 — AMOUNT_MISMATCH 예외 발생 (위변조 방어)")
    void verifyAmount_mismatch_throwsException() {
        // given
        given(orderRepository.findByOrderId("order-uuid-1234")).willReturn(Optional.of(sampleOrder));

        // when & then
        assertThatThrownBy(() -> orderService.verifyAmount("order-uuid-1234", 9_999L))
                .isInstanceOf(TossPaymentsException.class)
                .satisfies(ex -> {
                    TossPaymentsException tpe = (TossPaymentsException) ex;
                    assertThat(tpe.getCode()).isEqualTo("AMOUNT_MISMATCH");
                    assertThat(tpe.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("존재하지 않는 orderId — NOT_FOUND_ORDER 예외 발생")
    void verifyAmount_notFound_throwsException() {
        // given
        given(orderRepository.findByOrderId("unknown-order")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.verifyAmount("unknown-order", 10_000L))
                .isInstanceOf(TossPaymentsException.class)
                .satisfies(ex -> {
                    TossPaymentsException tpe = (TossPaymentsException) ex;
                    assertThat(tpe.getCode()).isEqualTo("NOT_FOUND_ORDER");
                    assertThat(tpe.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private OrderPrepareRequest mockRequest(String orderId, String orderName, Long amount) {
        OrderPrepareRequest req = new OrderPrepareRequest();
        ReflectionTestUtils.setField(req, "orderId", orderId);
        ReflectionTestUtils.setField(req, "orderName", orderName);
        ReflectionTestUtils.setField(req, "amount", amount);
        ReflectionTestUtils.setField(req, "customerEmail", "test@example.com");
        ReflectionTestUtils.setField(req, "customerName", "홍길동");
        return req;
    }
}
