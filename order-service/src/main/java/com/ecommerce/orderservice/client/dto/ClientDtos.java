package com.ecommerce.orderservice.client.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contractele cu celelalte servicii. Sunt o propunere -- trebuie confirmate
 * cu colegii care implementeaza cart-, inventory-, payment-service.
 */
public final class ClientDtos {

    private ClientDtos() {
    }

    // ---- cart-service ----
    public record CartResponse(String userId, List<CartItem> items) {
    }

    public record CartItem(String productId, String sellerId, String productName, BigDecimal unitPrice, int quantity) {
    }

    // ---- inventory-service ----
    public record ReservationRequest(UUID orderId, List<ReservationItem> items) {
    }

    public record ReservationItem(String productId, int quantity) {
    }

    public record ReservationResponse(String reservationId) {
    }

    // ---- payment-service ----
    public record PaymentRequest(UUID orderId, String userId, BigDecimal amount, String currency) {
    }

    public record PaymentResponse(String paymentId, String status, String failureReason) {

        public boolean succeeded() {
            return "SUCCEEDED".equals(status);
        }
    }

    // ---- notification-service ----
    public record NotificationRequest(String userId, String type, Map<String, String> data) {
    }
}
