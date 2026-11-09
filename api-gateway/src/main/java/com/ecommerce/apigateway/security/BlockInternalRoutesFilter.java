package com.ecommerce.apigateway.security;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Refuza cu 403 orice cerere catre rutele interne, inainte de autentificare.
 * <p>
 * WebFilter (nu GlobalFilter): /internal nu are ruta in gateway, iar un GlobalFilter ruleaza doar pe rutele
 * potrivite si dupa toate WebFilter-ele. Sunt refuzate si caile cu "..", pe care microserviciul le-ar normaliza
 * catre o ruta interna (ex. /api/payments/%2e%2e/%2e%2e/internal/payments).
 */
@Component
public class BlockInternalRoutesFilter implements WebFilter, Ordered {

    private static final String INTERNAL_SEGMENT = "internal";
    private static final String PARENT_SEGMENT = "..";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isInternalPath(exchange.getRequest().getPath().value())) {
            return forbidden(exchange);
        }
        return chain.filter(exchange);
    }

    /** Compara pe segmente decodate, ca /%69nternal sau /internal;x=1 sa nu scape, iar /api/internal-docs sa treaca. */
    boolean isInternalPath(String path) {
        String decoded;
        try {
            decoded = UriUtils.decode(path, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return true; // cale care nu poate fi interpretata: o refuzam
        }
        for (String segment : decoded.split("[/\\\\]")) {
            String name = segment.split(";", 2)[0];
            if (INTERNAL_SEGMENT.equalsIgnoreCase(name) || PARENT_SEGMENT.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        String body = "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Rutele interne nu sunt accesibile din exterior\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
