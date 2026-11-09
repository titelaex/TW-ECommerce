package com.ecommerce.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Verifica token-ul Firebase pe fiecare cerere, inainte de rutare.
 * <p>
 * WebFilter (nu GlobalFilter) ca sa se aplice si endpoint-urilor din gateway (ex. /api/auth/me),
 * nu doar rutelor catre microservicii.
 */
@Component
public class FirebaseAuthFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthFilter.class);
    private static final String BEARER = "Bearer ";
    private static final String DEV_TOKEN_PREFIX = "dev-token:";

    /** Cai accesibile fara token. */
    private static final List<String> PUBLIC_PATHS = List.of("/actuator/health", "/actuator/info");

    /** Catalogul se poate rasfoi fara cont, dar doar pentru citire. */
    private static final List<String> PUBLIC_GET_PREFIXES = List.of("/api/products", "/api/categories");

    private final TokenVerifier verifier;
    private final boolean devMode;

    public FirebaseAuthFilter(TokenVerifier verifier, @Value("${firebase.dev-mode:false}") boolean devMode) {
        this.verifier = verifier;
        this.devMode = devMode;
        if (devMode) {
            log.warn("Mod dev activ: gateway-ul accepta token-uri dev-token:<userId>:<role> fara verificare.");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Clientul nu are voie sa trimita singur X-User-*: le stergem mereu, ca sa nu poata fi falsificate.
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(h -> List.copyOf(h.keySet()).stream()
                        .filter(name -> name.regionMatches(true, 0, UserHeaders.PREFIX, 0, UserHeaders.PREFIX.length()))
                        .forEach(h::remove))
                .build();
        ServerWebExchange cleanExchange = exchange.mutate().request(stripped).build();

        if (isPublicRoute(stripped.getPath().value(), stripped.getMethod())) {
            return chain.filter(cleanExchange);
        }

        String authHeader = stripped.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            return unauthorized(cleanExchange, "Lipseste header-ul Authorization: Bearer <token>");
        }
        String idToken = authHeader.substring(BEARER.length()).trim();

        if (devMode && idToken.startsWith(DEV_TOKEN_PREFIX)) {
            return handleDevToken(cleanExchange, chain, idToken);
        }

        // verifyIdToken e blocant (poate descarca cheile publice Google), deci nu-l rulam pe event loop.
        return Mono.fromCallable(() -> verifyOrThrow(idToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(user -> forwardAs(cleanExchange, chain, user))
                .onErrorResume(TokenVerificationException.class, e -> unauthorized(cleanExchange, e.getMessage()));
    }

    /** Format: dev-token:&lt;userId&gt;:&lt;role&gt;. Ajunge aici doar cu firebase.dev-mode=true. */
    private Mono<Void> handleDevToken(ServerWebExchange exchange, WebFilterChain chain, String token) {
        String[] parts = token.split(":", -1);
        if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank()) {
            return unauthorized(exchange, "Format invalid; foloseste dev-token:<userId>:<role>");
        }
        return forwardAs(exchange, chain, new AuthenticatedUser(parts[1], null, parts[2]));
    }

    private Mono<Void> forwardAs(ServerWebExchange exchange, WebFilterChain chain, AuthenticatedUser user) {
        ServerHttpRequest withUser = exchange.getRequest().mutate()
                .header(UserHeaders.ID, user.uid())
                .header(UserHeaders.ROLE, user.role())
                .headers(h -> {
                    if (user.email() != null) {
                        h.set(UserHeaders.EMAIL, user.email());
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(withUser).build());
    }

    private AuthenticatedUser verifyOrThrow(String idToken) {
        try {
            return verifier.verify(idToken);
        } catch (Exception e) {
            log.debug("Token Firebase respins: {}", e.getMessage());
            throw new TokenVerificationException(e);
        }
    }

    private boolean isPublicRoute(String path, HttpMethod method) {
        if (HttpMethod.OPTIONS.equals(method)) {
            return true; // preflight CORS
        }
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        return HttpMethod.GET.equals(method) && PUBLIC_GET_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        String body = "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"" + message.replace("\"", "'") + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /** Ambalam orice eroare de verificare, ca erorile din aval (microservicii) sa nu devina 401. */
    static class TokenVerificationException extends RuntimeException {
        TokenVerificationException(Throwable cause) {
            super("Token invalid sau expirat", cause);
        }
    }

}
