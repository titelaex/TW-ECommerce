package com.ecommerce.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FirebaseAuthFilterTest {

    private static final String VALID = "valid-token";

    private final FirebaseAuthFilter filter = new FirebaseAuthFilter(token -> {
        if (!VALID.equals(token)) {
            throw new IllegalArgumentException("bad token");
        }
        return new AuthenticatedUser("uid-123", "client@test.com", "CUSTOMER");
    });

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final WebFilterChain chain = ex -> {
        forwarded.set(ex);
        return Mono.empty();
    };

    @Test
    void missingTokenIsRejected() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/me"));
        filter.filter(exchange, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void invalidTokenIsRejected() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer nope"));
        filter.filter(exchange, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void validTokenAddsUserHeadersAndOverridesSpoofedOnes() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID)
                .header(UserHeaders.ID, "hacker")
                .header(UserHeaders.ROLE, "ADMIN")
                .header("x-user-anything", "spoof"));
        filter.filter(exchange, chain).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertEquals("uid-123", headers.getFirst(UserHeaders.ID));
        assertEquals("CUSTOMER", headers.getFirst(UserHeaders.ROLE));
        assertEquals("client@test.com", headers.getFirst(UserHeaders.EMAIL));
        assertEquals(1, headers.get(UserHeaders.ID).size());
        assertNull(headers.getFirst("X-User-Anything"));
    }

    @Test
    void healthIsPublicButSpoofedHeadersAreStillRemoved() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health")
                .header(UserHeaders.ID, "hacker"));
        filter.filter(exchange, chain).block();
        assertNull(forwarded.get().getRequest().getHeaders().getFirst(UserHeaders.ID));
    }
}
