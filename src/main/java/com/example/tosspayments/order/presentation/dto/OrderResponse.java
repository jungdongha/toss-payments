package com.example.tosspayments.order.presentation.dto;

import com.example.tosspayments.order.domain.Order;
import com.example.tosspayments.order.domain.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 주문 조회 응답 DTO
 *
 * JPA Entity를 Presentation Layer에 직접 노출하지 않기 위한 DTO입니다.
 */
@Getter
public class OrderResponse {

    private final String orderId;
    private final String orderName;
    private final Long amount;
    private final String customerEmail;
    private final String customerName;
    private final OrderStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private OrderResponse(Order order) {
        this.orderId       = order.getOrderId();
        this.orderName     = order.getOrderName();
        this.amount        = order.getAmount();
        this.customerEmail = order.getCustomerEmail();
        this.customerName  = order.getCustomerName();
        this.status        = order.getStatus();
        this.createdAt     = order.getCreatedAt();
        this.updatedAt     = order.getUpdatedAt();
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order);
    }
}
