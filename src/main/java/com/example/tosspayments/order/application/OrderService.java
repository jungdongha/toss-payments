package com.example.tosspayments.order.application;

import com.example.tosspayments.global.exception.TossPaymentsException;
import com.example.tosspayments.order.domain.Order;
import com.example.tosspayments.order.domain.OrderRepository;
import com.example.tosspayments.order.presentation.dto.OrderPrepareRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * 결제 요청 전 주문 임시 저장
     *
     * 결제 승인 시 verifyAmount()로 금액을 재검증하여 위변조를 방지합니다.
     */
    @Transactional
    public Order prepareOrder(OrderPrepareRequest request) {
        if (orderRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new TossPaymentsException(
                    "ALREADY_EXISTS_ORDER",
                    "이미 존재하는 주문번호입니다: " + request.getOrderId(),
                    HttpStatus.CONFLICT
            );
        }

        Order order = Order.builder()
                .orderId(request.getOrderId())
                .orderName(request.getOrderName())
                .amount(request.getAmount())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .build();

        Order saved = orderRepository.save(order);
        log.info("[Order] 임시 저장 완료 — orderId: {}, amount: {}", saved.getOrderId(), saved.getAmount());
        return saved;
    }

    /**
     * 결제 승인 전 금액 검증
     *
     * 클라이언트에서 넘어온 amount와 DB 저장 금액을 비교합니다.
     */
    public Order verifyAmount(String orderId, Long requestedAmount) {
        Order order = getByOrderId(orderId);

        if (!order.getAmount().equals(requestedAmount)) {
            log.warn("[Order] 금액 불일치! orderId: {}, DB: {}, 요청: {}",
                    orderId, order.getAmount(), requestedAmount);
            throw new TossPaymentsException(
                    "AMOUNT_MISMATCH",
                    "결제 금액이 주문 금액과 다릅니다. (DB: " + order.getAmount() + ", 요청: " + requestedAmount + ")"
            );
        }

        return order;
    }

    public Order getByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new TossPaymentsException(
                        "NOT_FOUND_ORDER",
                        "주문 정보를 찾을 수 없습니다: " + orderId,
                        HttpStatus.NOT_FOUND
                ));
    }
}
