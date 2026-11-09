package com.ecommerce.paymentservice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Aici se valideaza doar forma cererii; regulile despre suma si moneda sunt in FraudCheckService. */
public record PaymentRequest(@NotBlank @Size(max = 255) String orderId,
                             @NotBlank @Size(max = 255) String userId,
                             @NotNull @Digits(integer = 10, fraction = 2) BigDecimal amount,
                             @NotBlank @Size(max = 3) String currency) {
}
