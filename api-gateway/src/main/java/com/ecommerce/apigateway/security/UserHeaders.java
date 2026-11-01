package com.ecommerce.apigateway.security;

/** Header-ele pe care gateway-ul le trimite mai departe catre microservicii. */
public final class UserHeaders {

    public static final String PREFIX = "X-User-";
    public static final String ID = "X-User-Id";
    public static final String EMAIL = "X-User-Email";
    public static final String ROLE = "X-User-Role";

    private UserHeaders() {
    }
}
