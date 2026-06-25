package com.example.tosspayments.payment.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 토스페이먼츠 Payment 객체 응답 DTO
 * API 버전: 2022-11-16
 * 참고: https://docs.tosspayments.com/reference
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private String mId;
    private String version;
    private String paymentKey;
    private String type;            // NORMAL, BILLING, BRANDPAY
    private String orderId;
    private String orderName;
    private String currency;
    private String method;          // 카드, 간편결제, 가상계좌, 계좌이체, 휴대폰 등
    private String status;          // DONE, CANCELED, PARTIAL_CANCELED, WAITING_FOR_DEPOSIT …

    private Long totalAmount;
    private Long balanceAmount;
    private Long suppliedAmount;
    private Long vat;
    private Long taxFreeAmount;
    private Long taxExemptionAmount;

    private OffsetDateTime requestedAt;
    private OffsetDateTime approvedAt;

    private Boolean useEscrow;
    private Boolean cultureExpense;
    private Boolean isPartialCancelable;

    /** 가상계좌 웹훅 진위 검증용 — 반드시 DB 저장 */
    private String secret;

    // ── 결제수단별 상세 ───────────────────────────

    private Card            card;
    private VirtualAccount  virtualAccount;
    private MobilePhone     mobilePhone;
    private GiftCertificate giftCertificate;
    private EasyPay         easyPay;
    private Transfer        transfer;
    private Discount        discount;
    private Checkout        checkout;
    private Receipt         receipt;

    /** 취소/환불 내역 */
    private List<Cancel> cancels;

    // ── Inner DTOs ────────────────────────────────

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Card {
        private String  issuerCode;
        private String  acquirerCode;
        private String  number;
        private Integer installmentPlanMonths;
        private String  approveNo;
        private Boolean useCardPoint;
        private String  cardType;
        private String  ownerType;
        private String  acquireStatus;
        private Boolean isInterestFree;
        private String  interestPayer;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VirtualAccount {
        private String  accountType;
        private String  accountNumber;
        private String  bankCode;
        private String  customerName;
        private String  dueDate;
        private String  refundStatus;
        private Boolean expired;
        private String  settlementStatus;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MobilePhone {
        private String customerMobilePhone;
        private String settlementStatus;
        private String receiptUrl;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GiftCertificate {
        private String approveNo;
        private String settlementStatus;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EasyPay {
        private String provider;        // 토스페이, 네이버페이, 카카오페이 …
        private Long   amount;
        private Long   discountAmount;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transfer {
        private String bankCode;
        private String settlementStatus;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Discount {
        private Long amount;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Checkout {
        private String url;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Receipt {
        private String url;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cancel {
        private Long           cancelAmount;
        private String         cancelReason;
        private Long           taxFreeAmount;
        private Long           taxExemptionAmount;
        private Long           refundableAmount;
        private Long           easyPayDiscountAmount;
        private OffsetDateTime canceledAt;
        private String         transactionKey;
        private String         receiptKey;
        private String         cancelStatus;
    }
}
