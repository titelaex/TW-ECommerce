package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** Trimis de order-service catre POST /internal/notifications. */
public record SendNotificationRequest(
        @NotBlank String userId,
        @NotNull NotificationType type,
        Map<String, String> data) {
}
