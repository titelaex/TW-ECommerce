package com.ecommerce.orderservice.exception;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String details) {
        super("Stoc insuficient: " + details);
    }
}
