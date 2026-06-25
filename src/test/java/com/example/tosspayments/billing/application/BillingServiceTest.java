package com.example.tosspayments.billing.application;

import com.example.tosspayments.billing.presentation.dto.BillingAuthorizeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingChargeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingResponse;
import com.example.tosspayments.payment.domain.Payment;
import com.example.tosspayments.payment.domain.PaymentRepository;
import com.example.tosspayments.payment.domain.PaymentStatus;
import com.example.tosspayments.payment.infrastructure.TossPaymentsClient;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService 단위 테스트")
class BillingServiceTest {

    @Mock private TossPaymentsClient tossClient;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private BillingService billingService;

    private BillingAuthorizeRequest authorizeRequest;
    private BillingChargeRequest chargeRequest;
    private BillingResponse billingResponse;
    private PaymentResponse chargeResponse;

    @BeforeEach
    void setUp() {
        authorizeRequest = new BillingAuthorizeRequest();
        ReflectionTestUtils.setField(authorizeRequest, "authKey", "auth-key-abc");
        ReflectionTestUtils.setField(authorizeRequest, "customerKey", "customer-key-123");

        billingResponse = new BillingResponse();
        ReflectionTestUtils.setField(billingResponse, "billingKey", "billing-key-xyz");
        ReflectionTestUtils.setField(billingResponse, "customerKey", "customer-key-123");

        chargeRequest = new BillingChargeRequest();
        ReflectionTestUtils.setField(chargeRequest, "customerKey", "customer-key-123");
        ReflectionTestUtils.setField(chargeRequest, "amount", 9_900L);
        ReflectionTestUtils.setField(chargeRequest, "orderId", "sub-order-001");
        ReflectionTestUtils.setField(chargeRequest, "orderName", "월 구독 결제");
        ReflectionTestUtils.setField(chargeRequest, "customerEmail", "member@example.com");
        ReflectionTestUtils.setField(chargeRequest, "customerName", "구독자");

        chargeResponse = new PaymentResponse();
        ReflectionTestUtils.setField(chargeResponse, "paymentKey", "toss_billing_pk_001");
        ReflectionTestUtils.setField(chargeResponse, "orderId", "sub-order-001");
        ReflectionTestUtils.setField(chargeResponse, "orderName", "월 구독 결제");
        ReflectionTestUtils.setField(chargeResponse, "totalAmount", 9_900L);
        ReflectionTestUtils.setField(chargeResponse, "status", "DONE");
        ReflectionTestUtils.setField(chargeResponse, "method", "카드");
    }

    @Test
    @DisplayName("빌링키 발급 — TossClient 위임 후 응답 반환")
    void issueBillingKey_delegatesToClient() {
        // given
        given(tossClient.issueBillingKey(authorizeRequest)).willReturn(billingResponse);

        // when
        BillingResponse result = billingService.issueBillingKey(authorizeRequest);

        // then
        assertThat(result.getBillingKey()).isEqualTo("billing-key-xyz");
        assertThat(result.getCustomerKey()).isEqualTo("customer-key-123");
        then(tossClient).should(times(1)).issueBillingKey(authorizeRequest);
    }

    @Test
    @DisplayName("자동결제 실행 — Payment DB 저장 확인")
    void charge_savesPaymentToRepository() {
        // given
        given(tossClient.chargeBilling("billing-key-xyz", chargeRequest)).willReturn(chargeResponse);
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        PaymentResponse result = billingService.charge("billing-key-xyz", chargeRequest);

        // then
        assertThat(result.getPaymentKey()).isEqualTo("toss_billing_pk_001");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        then(paymentRepository).should().save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getPaymentKey()).isEqualTo("toss_billing_pk_001");
        assertThat(saved.getOrderId()).isEqualTo("sub-order-001");
        assertThat(saved.getAmount()).isEqualTo(9_900L);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(saved.getCustomerEmail()).isEqualTo("member@example.com");
    }
}
