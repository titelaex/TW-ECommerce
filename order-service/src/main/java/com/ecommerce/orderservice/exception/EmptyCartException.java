package com.ecommerce.orderservice.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Cosul este gol");
    }
}
