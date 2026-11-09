package com.ecommerce.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Produsele nu vin de la client: order-service le citeste din cart-service,
 * ca preturile sa nu poata fi manipulate din frontend.
 */
public record CheckoutRequest(@NotNull @Valid AddressDto shippingAddress) {

    public record AddressDto(
            @NotBlank String fullName,
            @NotBlank String phone,
            @NotBlank String street,
            @NotBlank String city,
            @NotBlank String county,
            @NotBlank String postalCode) {
    }
}
