package com.ecommerce.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** Cai accesibile fara token. */
    private static final List<String> PUBLIC_PATHS = List.of("/actuator/health", "/actuator/info");

    private final TokenVerifier verifier;

    public FirebaseAuthFilter(TokenVerifier verifier) {
        this.verifier = verifier;
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

        if (isPublic(stripped)) {
            return chain.filter(cleanExchange);
        }

        String authHeader = stripped.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            return unauthorized(cleanExchange, "Lipseste header-ul Authorization: Bearer <token>");
        }
        String idToken = authHeader.substring(BEARER.length()).trim();

        // verifyIdToken e blocant (poate descarca cheile publice Google), deci nu-l rulam pe event loop.
        return Mono.fromCallable(() -> verifyOrThrow(idToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(user -> {
                    ServerHttpRequest withUser = stripped.mutate()
                            .header(UserHeaders.ID, user.uid())
                            .header(UserHeaders.ROLE, user.role())
                            .headers(h -> {
                                if (user.email() != null) {
                                    h.set(UserHeaders.EMAIL, user.email());
                                }
                            })
                            .build();
                    return chain.filter(cleanExchange.mutate().request(withUser).build());
                })
                .onErrorResume(TokenVerificationException.class, e -> unauthorized(cleanExchange, e.getMessage()));
    }

    private AuthenticatedUser verifyOrThrow(String idToken) {
        try {
            return verifier.verify(idToken);
        } catch (Exception e) {
            log.debug("Token Firebase respins: {}", e.getMessage());
            throw new TokenVerificationException(e);
        }
    }

    private boolean isPublic(ServerHttpRequest request) {
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return true; // preflight CORS
        }
        String path = request.getPath().value();
        return PUBLIC_PATHS.contains(path);
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
