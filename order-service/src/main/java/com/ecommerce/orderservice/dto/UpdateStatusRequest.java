package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull OrderStatus status) {
}
