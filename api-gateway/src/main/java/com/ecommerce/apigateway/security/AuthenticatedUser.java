package com.ecommerce.apigateway.security;

/**
 * Utilizatorul extras dintr-un token Firebase valid.
 * Rolul vine din custom claim-ul "role"; daca lipseste, utilizatorul e CUSTOMER.
 */
public record AuthenticatedUser(String uid, String email, String role) {

    public static final String DEFAULT_ROLE = "CUSTOMER";
}
