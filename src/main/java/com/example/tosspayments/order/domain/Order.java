package com.example.tosspayments.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 Aggregate Root
 *
 * [역할]
 * 결제 요청 전 orderId와 amount를 서버 DB에 임시 저장합니다.
 * 토스페이먼츠 결제 승인 직전, 여기 저장된 amount로 클라이언트 요청값을 검증하여
 * 결제 금액 위변조 공격을 방지합니다.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주문 고유번호 (UUID) — 토스 orderId와 동일 */
    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String orderName;

    /** 결제 금액 — 승인 전 검증 기준값 */
    @Column(nullable = false)
    private Long amount;

    private String customerEmail;
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Order(String orderId, String orderName, Long amount,
                 String customerEmail, String customerName) {
        this.orderId = orderId;
        this.orderName = orderName;
        this.amount = amount;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.status = OrderStatus.PENDING;
    }

    // ── Domain Methods ──────────────────────────

    public void markAsPaid() {
        this.status = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCanceled() {
        this.status = OrderStatus.CANCELED;
        this.updatedAt = LocalDateTime.now();
    }
}
