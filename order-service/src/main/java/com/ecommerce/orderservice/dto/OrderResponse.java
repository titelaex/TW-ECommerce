package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<Item> items,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public record Item(String productId, String sellerId, String productName, BigDecimal unitPrice, int quantity) {

        static Item from(OrderItem item) {
            return new Item(item.getProductId(), item.getSellerId(), item.getProductName(),
                    item.getUnitPrice(), item.getQuantity());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getItems().stream().map(Item::from).toList(),
                order.getFailureReason(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
