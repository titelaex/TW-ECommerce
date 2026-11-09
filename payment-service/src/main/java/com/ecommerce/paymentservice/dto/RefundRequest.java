package com.ecommerce.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(@NotBlank String reason) {
}
