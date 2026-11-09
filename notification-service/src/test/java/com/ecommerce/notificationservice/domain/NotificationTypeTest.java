package com.ecommerce.notificationservice.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationTypeTest {

    @Test
    void rendersPlaceholdersFromData() {
        var data = Map.of("orderId", "A1", "total", "120.00 RON");

        assertEquals("Comanda confirmata", NotificationType.ORDER_CONFIRMED.title(data));
        assertEquals("Comanda A1 a fost platita. Total: 120.00 RON.", NotificationType.ORDER_CONFIRMED.body(data));
    }
}
