package com.ecommerce.paymentservice.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID id) {
        super("Plata " + id + " nu exista");
    }

    public PaymentNotFoundException(String orderId) {
        super("Nu exista plata pentru comanda " + orderId);
    }
}
