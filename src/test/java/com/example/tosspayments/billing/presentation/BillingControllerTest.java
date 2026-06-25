package com.example.tosspayments.billing.presentation;

import com.example.tosspayments.billing.application.BillingService;
import com.example.tosspayments.billing.presentation.dto.BillingAuthorizeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingChargeRequest;
import com.example.tosspayments.billing.presentation.dto.BillingResponse;
import com.example.tosspayments.global.exception.GlobalExceptionHandler;
import com.example.tosspayments.payment.presentation.dto.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillingController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("BillingController 슬라이스 테스트")
class BillingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean BillingService billingService;

    // ── POST /billing/authorize ────────────────────────────────────────────

    @Test
    @DisplayName("빌링키 발급 성공 — 200 OK + billingKey 반환")
    void authorize_success_returns200() throws Exception {
        BillingResponse response = new BillingResponse();
        ReflectionTestUtils.setField(response, "billingKey", "billing-key-xyz");
        ReflectionTestUtils.setField(response, "customerKey", "customer-key-123");

        given(billingService.issueBillingKey(any(BillingAuthorizeRequest.class))).willReturn(response);

        Map<String, String> body = Map.of(
                "authKey", "auth-key-abc",
                "customerKey", "customer-key-123"
        );

        mockMvc.perform(post("/billing/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingKey").value("billing-key-xyz"))
                .andExpect(jsonPath("$.customerKey").value("customer-key-123"));
    }

    @Test
    @DisplayName("빌링키 발급 — authKey 누락 시 400 반환 (@Valid 검증)")
    void authorize_missingAuthKey_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "customerKey", "customer-key-123"
                // authKey 누락
        );

        mockMvc.perform(post("/billing/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.authKey").exists());
    }

    // ── POST /billing/{billingKey}/charge ──────────────────────────────────

    @Test
    @DisplayName("자동결제 실행 성공 — 200 OK + paymentKey 반환")
    void charge_success_returns200() throws Exception {
        PaymentResponse response = new PaymentResponse();
        ReflectionTestUtils.setField(response, "paymentKey", "toss_billing_pk_001");
        ReflectionTestUtils.setField(response, "orderId", "sub-order-001");
        ReflectionTestUtils.setField(response, "totalAmount", 9_900L);
        ReflectionTestUtils.setField(response, "status", "DONE");

        given(billingService.charge(eq("billing-key-xyz"), any(BillingChargeRequest.class)))
                .willReturn(response);

        Map<String, Object> body = Map.of(
                "customerKey", "customer-key-123",
                "amount", 9900,
                "orderId", "sub-order-001",
                "orderName", "월 구독 결제"
        );

        mockMvc.perform(post("/billing/billing-key-xyz/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentKey").value("toss_billing_pk_001"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("자동결제 실행 — amount 누락 시 400 반환 (@Valid 검증)")
    void charge_missingAmount_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "customerKey", "customer-key-123",
                "orderId", "sub-order-001",
                "orderName", "월 구독 결제"
                // amount 누락
        );

        mockMvc.perform(post("/billing/billing-key-xyz/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }
}
