package com.ecommerce.notificationservice.domain;

import java.time.Instant;
import java.util.Map;

/** Document din colectia Firestore `notifications`. */
public record Notification(
        String id,
        String userId,
        NotificationType type,
        String title,
        String body,
        Map<String, String> data,
        boolean read,
        DeliveryStatus deliveryStatus,
        Instant createdAt) {
}
