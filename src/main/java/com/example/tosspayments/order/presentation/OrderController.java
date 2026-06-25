package com.example.tosspayments.order.presentation;

import com.example.tosspayments.order.application.OrderService;
import com.example.tosspayments.order.domain.Order;
import com.example.tosspayments.order.presentation.dto.OrderPrepareRequest;
import com.example.tosspayments.order.presentation.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 주문 API
 *
 * [결제 흐름에서의 역할]
 * 1. POST /orders/prepare  — 결제 버튼 클릭 시 주문 금액을 서버에 임시 저장
 * 2. 이후 결제 승인(POST /payments/confirm) 시 여기 저장된 금액으로 위변조 검증
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/prepare")
    public ResponseEntity<Map<String, Object>> prepareOrder(
            @Valid @RequestBody OrderPrepareRequest request) {
        Order order = orderService.prepareOrder(request);
        return ResponseEntity.ok(Map.of(
                "orderId",   order.getOrderId(),
                "orderName", order.getOrderName(),
                "amount",    order.getAmount()
        ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(OrderResponse.from(orderService.getByOrderId(orderId)));
    }
}
