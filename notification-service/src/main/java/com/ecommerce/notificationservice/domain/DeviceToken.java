package com.ecommerce.notificationservice.domain;

import java.time.Instant;

/** Document din colectia Firestore `device_tokens`; id-ul documentului este token-ul FCM. */
public record DeviceToken(String token, String userId, String platform, Instant updatedAt) {
}
