package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.domain.Payment;
import com.ecommerce.paymentservice.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID paymentId,
                              String orderId,
                              String userId,
                              BigDecimal amount,
                              String currency,
                              PaymentStatus status,
                              String failureReason,
                              String txnRef,
                              Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getTransactionReference(),
                payment.getCreatedAt());
    }
}
