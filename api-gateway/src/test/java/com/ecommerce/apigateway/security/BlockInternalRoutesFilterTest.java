package com.ecommerce.apigateway.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockInternalRoutesFilterTest {

    private final BlockInternalRoutesFilter filter = new BlockInternalRoutesFilter();

    private final AtomicBoolean forwarded = new AtomicBoolean(false);
    private final WebFilterChain chain = ex -> {
        forwarded.set(true);
        return Mono.empty();
    };

    /** URI.create pastreaza calea exact asa cum vine de la client, inclusiv codarile %xx. */
    private MockServerWebExchange run(String rawPath) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, URI.create(rawPath)));
        filter.filter(exchange, chain).block();
        return exchange;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/internal",
            "/internal/payments",
            "/INTERNAL/payments",
            "/payment-service/internal/payments",
            "/%69nternal/payments",
            "/internal;x=1/payments",
            "/api/payments/..%2Finternal/payments",
            "/api/payments/%2e%2e/%2e%2e/internal/payments",
            "/api/products/%2e%2e/orders"
    })
    void internalOrTraversalPathsAreForbidden(String path) {
        var exchange = run(path);

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(forwarded.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/payments/123",
            "/api/payments/my-history",
            "/api/internal-docs",
            "/api/products/internals",
            "/actuator/health"
    })
    void otherPathsPassThrough(String path) {
        var exchange = run(path);

        assertNull(exchange.getResponse().getStatusCode());
        assertTrue(forwarded.get());
    }
}
