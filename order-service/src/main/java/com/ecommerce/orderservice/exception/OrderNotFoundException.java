package com.ecommerce.orderservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Comanda " + id + " nu exista");
    }
}
