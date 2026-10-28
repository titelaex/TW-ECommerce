package com.ecommerce.orderservice.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.STOCK_RESERVED));
        assertTrue(OrderStatus.STOCK_RESERVED.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void cannotSkipPaymentOrLeaveTerminalStates() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.PAYMENT_FAILED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }
}
