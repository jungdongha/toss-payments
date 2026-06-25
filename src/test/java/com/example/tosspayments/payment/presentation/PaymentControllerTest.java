package com.example.tosspayments.payment.presentation;

import com.example.tosspayments.global.exception.GlobalExceptionHandler;
import com.example.tosspayments.global.exception.TossPaymentsException;
import com.example.tosspayments.payment.application.PaymentService;
import com.example.tosspayments.payment.presentation.dto.PaymentConfirmRequest;
import com.example.tosspayments.payment.presentation.dto.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("PaymentController 슬라이스 테스트")
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean PaymentService paymentService;

    // ── POST /payments/confirm ──────────────────────────────────────────────

    @Test
    @DisplayName("결제 승인 성공 — 200 OK + paymentKey 반환")
    void confirm_success_returns200() throws Exception {
        PaymentResponse response = new PaymentResponse();
        ReflectionTestUtils.setField(response, "paymentKey", "toss_pk_001");
        ReflectionTestUtils.setField(response, "orderId", "order-uuid-1");
        ReflectionTestUtils.setField(response, "totalAmount", 15_000L);
        ReflectionTestUtils.setField(response, "status", "DONE");

        given(paymentService.confirm(any(PaymentConfirmRequest.class))).willReturn(response);

        Map<String, Object> body = Map.of(
                "paymentKey", "toss_pk_001",
                "orderId", "order-uuid-1",
                "amount", 15_000
        );

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentKey").value("toss_pk_001"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("결제 승인 — paymentKey 누락 시 400 반환 (@Valid 검증)")
    void confirm_missingPaymentKey_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "orderId", "order-uuid-1",
                "amount", 15_000
                // paymentKey 누락
        );

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.paymentKey").exists());
    }

    @Test
    @DisplayName("결제 승인 — amount 누락 시 400 반환 (@Valid 검증)")
    void confirm_missingAmount_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "paymentKey", "toss_pk_001",
                "orderId", "order-uuid-1"
                // amount 누락
        );

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    @DisplayName("결제 승인 — 금액 불일치 시 TossPaymentsException → 400 반환")
    void confirm_amountMismatch_returns400() throws Exception {
        given(paymentService.confirm(any())).willThrow(
                new TossPaymentsException("AMOUNT_MISMATCH", "결제 금액이 주문 금액과 다릅니다.")
        );

        Map<String, Object> body = Map.of(
                "paymentKey", "toss_pk_001",
                "orderId", "order-uuid-1",
                "amount", 1
        );

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
    }

    // ── GET /payments/{paymentKey} ─────────────────────────────────────────

    @Test
    @DisplayName("결제 단건 조회 — 200 OK")
    void getByPaymentKey_success_returns200() throws Exception {
        PaymentResponse response = new PaymentResponse();
        ReflectionTestUtils.setField(response, "paymentKey", "toss_pk_001");
        ReflectionTestUtils.setField(response, "status", "DONE");

        given(paymentService.getByPaymentKey("toss_pk_001")).willReturn(response);

        mockMvc.perform(get("/payments/toss_pk_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentKey").value("toss_pk_001"));
    }

    @Test
    @DisplayName("결제 단건 조회 — 존재하지 않는 paymentKey → 404 반환")
    void getByPaymentKey_notFound_returns404() throws Exception {
        given(paymentService.getByPaymentKey("unknown")).willThrow(
                new TossPaymentsException("NOT_FOUND_PAYMENT", "결제 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)
        );

        mockMvc.perform(get("/payments/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_PAYMENT"));
    }

    // ── GET /payments ──────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 내역 목록 조회 — 200 OK + 빈 배열")
    void getAll_emptyList_returns200() throws Exception {
        given(paymentService.getAll()).willReturn(List.of());

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
