package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.domain.OrderStatus;

import java.util.UUID;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(UUID id, OrderStatus from, OrderStatus to) {
        super("Comanda " + id + " nu poate trece din " + from + " in " + to);
    }
}
