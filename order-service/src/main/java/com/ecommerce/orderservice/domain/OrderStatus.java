package com.ecommerce.orderservice.domain;

public enum OrderStatus {
    /** Comanda a fost creata, stocul nu e inca rezervat. */
    PENDING,
    /** inventory-service a rezervat stocul. */
    STOCK_RESERVED,
    /** Plata a reusit, rezervarea a fost confirmata. */
    CONFIRMED,
    /** Stoc insuficient la rezervare. */
    REJECTED,
    /** Plata a esuat, rezervarea a fost eliberata. */
    PAYMENT_FAILED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == STOCK_RESERVED || next == REJECTED || next == CANCELLED;
            case STOCK_RESERVED -> next == CONFIRMED || next == PAYMENT_FAILED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case REJECTED, PAYMENT_FAILED, DELIVERED, CANCELLED -> false;
        };
    }
}
