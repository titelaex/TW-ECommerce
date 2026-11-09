package com.ecommerce.orderservice.web;

/**
 * Header-ele puse de api-gateway dupa validarea token-ului Firebase.
 * Serviciile nu sunt expuse direct, deci au incredere in aceste valori.
 */
public final class UserHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SELLER = "SELLER";
    public static final String ROLE_ADMIN = "ADMIN";

    private UserHeaders() {
    }
}
