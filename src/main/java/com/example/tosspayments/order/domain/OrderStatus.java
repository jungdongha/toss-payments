package com.example.tosspayments.order.domain;

public enum OrderStatus {
    PENDING,    // 결제 대기 (주문 임시 저장)
    PAID,       // 결제 완료
    FAILED,     // 결제 실패
    CANCELED    // 결제 취소/환불
}
