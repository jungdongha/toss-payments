package com.example.tosspayments.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 Aggregate Root
 *
 * paymentKey는 토스페이먼츠에서 발급하는 결제 식별자입니다.
 * 결제 승인 후 반드시 DB에 저장해야 하며, 취소/조회 시 사용됩니다.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토스 결제 고유키 — 반드시 저장 및 보관 */
    @Column(unique = true)
    private String paymentKey;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String orderName;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** 결제수단 (카드, 간편결제, 가상계좌 등) */
    private String method;

    private String customerEmail;
    private String customerName;

    /** 가상계좌 웹훅 진위 검증용 secret */
    private String secret;

    private LocalDateTime approvedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Payment(String paymentKey, String orderId, String orderName, Long amount,
                   PaymentStatus status, String method, String customerEmail,
                   String customerName, String secret, LocalDateTime approvedAt) {
        this.paymentKey    = paymentKey;
        this.orderId       = orderId;
        this.orderName     = orderName;
        this.amount        = amount;
        this.status        = status;
        this.method        = method;
        this.customerEmail = customerEmail;
        this.customerName  = customerName;
        this.secret        = secret;
        this.approvedAt    = approvedAt;
    }

    // ── Domain Methods ──────────────────────────

    public void updateStatus(PaymentStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
}
