package com.ecommerce.paymentservice.web;

import com.ecommerce.paymentservice.domain.PaymentStatus;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({InternalPaymentController.class, PaymentController.class})
class PaymentEndpointsTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String VALID_BODY =
            "{\"orderId\":\"order-1\",\"userId\":\"user-1\",\"amount\":100.50,\"currency\":\"RON\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PaymentService service;

    private PaymentResponse succeeded() {
        return new PaymentResponse(ID, "order-1", "user-1", new BigDecimal("100.50"), "RON",
                PaymentStatus.SUCCEEDED, null, "TXN-1", Instant.parse("2026-11-09T10:00:00Z"));
    }

    private PaymentResponse failed() {
        return new PaymentResponse(ID, "order-1", "user-1", new BigDecimal("100.50"), "RON",
                PaymentStatus.FAILED, "Cont blocat", null, Instant.parse("2026-11-09T10:00:00Z"));
    }

    private PaymentResponse refunded() {
        return new PaymentResponse(ID, "order-1", "user-1", new BigDecimal("100.50"), "RON",
                PaymentStatus.REFUNDED, null, "TXN-1", Instant.parse("2026-11-09T10:00:00Z"));
    }

    @Test
    void processPaymentReturnsPaymentIdStatusAndTxnRef() throws Exception {
        when(service.processPayment(new PaymentRequest("order-1", "user-1", new BigDecimal("100.50"), "RON")))
                .thenReturn(succeeded());

        mvc.perform(post("/internal/payments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(ID.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.txnRef").value("TXN-1"));
    }

    @Test
    void refusedPaymentIsStill200WithStatusFailed() throws Exception {
        when(service.processPayment(any(PaymentRequest.class))).thenReturn(failed());

        mvc.perform(post("/internal/payments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Cont blocat"));
    }

    @Test
    void zeroAmountReachesFraudCheckInsteadOfBeing400() throws Exception {
        when(service.processPayment(any(PaymentRequest.class))).thenReturn(failed());

        mvc.perform(post("/internal/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"userId\":\"user-1\",\"amount\":0,\"currency\":\"RON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"userId\":\"user-1\",\"amount\":100.50,\"currency\":\"RON\"}",
            "{\"orderId\":\" \",\"userId\":\"user-1\",\"amount\":100.50,\"currency\":\"RON\"}",
            "{\"orderId\":\"order-1\",\"amount\":100.50,\"currency\":\"RON\"}",
            "{\"orderId\":\"order-1\",\"userId\":\"user-1\",\"currency\":\"RON\"}",
            "{\"orderId\":\"order-1\",\"userId\":\"user-1\",\"amount\":100.50}",
            "{\"orderId\":\"order-1\",\"userId\":\"user-1\",\"amount\":12345678901.00,\"currency\":\"RON\"}",
            "{\"orderId\":\"order-1\",\"userId\":\"user-1\",\"amount\":100.505,\"currency\":\"RON\"}",
            "{\"orderId\":\"order-1\",\"userId\":\"user-1\",\"amount\":100.50,\"currency\":\"EURO\"}"
    })
    void malformedPaymentRequestIs400(String body) throws Exception {
        mvc.perform(post("/internal/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void internalGetReturnsPayment() throws Exception {
        when(service.getPaymentById(ID)).thenReturn(succeeded());

        mvc.perform(get("/internal/payments/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-1"));
    }

    @Test
    void internalGetOfUnknownPaymentIs404() throws Exception {
        when(service.getPaymentById(ID)).thenThrow(new PaymentNotFoundException(ID));

        mvc.perform(get("/internal/payments/{id}", ID)).andExpect(status().isNotFound());
    }

    @Test
    void internalGetByOrderReturnsPayment() throws Exception {
        when(service.getPaymentByOrderId("order-1")).thenReturn(succeeded());

        mvc.perform(get("/internal/payments/orders/{orderId}", "order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(ID.toString()));
    }

    @Test
    void refundReturnsRefundedPayment() throws Exception {
        when(service.refundPayment(ID, "comanda anulata")).thenReturn(refunded());

        mvc.perform(post("/internal/payments/{id}/refund", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"comanda anulata\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void refundWithoutReasonIs400() throws Exception {
        mvc.perform(post("/internal/payments/{id}/refund", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundOfFailedPaymentIs409() throws Exception {
        when(service.refundPayment(ID, "comanda anulata"))
                .thenThrow(new InvalidPaymentStateException(ID, PaymentStatus.FAILED, PaymentStatus.REFUNDED));

        mvc.perform(post("/internal/payments/{id}/refund", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"comanda anulata\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentRefundConflictIs409() throws Exception {
        when(service.refundPayment(ID, "comanda anulata"))
                .thenThrow(new ObjectOptimisticLockingFailureException("Payment", ID));

        mvc.perform(post("/internal/payments/{id}/refund", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"comanda anulata\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCanReadOwnPayment() throws Exception {
        when(service.getPaymentById(ID)).thenReturn(succeeded());

        mvc.perform(get("/api/payments/{id}", ID).header("X-User-Id", "user-1").header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(ID.toString()));
    }

    @Test
    void adminCanReadAnyPayment() throws Exception {
        when(service.getPaymentById(ID)).thenReturn(succeeded());

        mvc.perform(get("/api/payments/{id}", ID).header("X-User-Id", "admin-1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void otherCustomerCannotReadPayment() throws Exception {
        when(service.getPaymentById(ID)).thenReturn(succeeded());

        mvc.perform(get("/api/payments/{id}", ID).header("X-User-Id", "user-2").header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void myHistoryReturnsPaymentsOfUserFromHeader() throws Exception {
        when(service.getPaymentsByUserId("user-1")).thenReturn(List.of(succeeded()));

        mvc.perform(get("/api/payments/my-history").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value("order-1"));
    }
}
