package com.ecommerce.apigateway.security;

/** Verifica un Firebase ID token. Interfata separata ca filtrul sa poata fi testat fara Firebase. */
@FunctionalInterface
public interface TokenVerifier {

    /** @throws Exception daca token-ul e invalid, expirat sau din alt proiect */
    AuthenticatedUser verify(String idToken) throws Exception;
}
