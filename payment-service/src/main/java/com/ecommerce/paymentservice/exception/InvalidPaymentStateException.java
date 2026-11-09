package com.ecommerce.paymentservice.exception;

import com.ecommerce.paymentservice.domain.PaymentStatus;

import java.util.UUID;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(UUID id, PaymentStatus from, PaymentStatus to) {
        super("Plata " + id + " nu poate trece din " + from + " in " + to);
    }
}
