package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.ClientDtos.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;

    public NotificationClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://notification-service").build();
    }

    /**
     * Best-effort: o notificare esuata nu trebuie sa strice o comanda deja platita.
     */
    public void notify(String userId, String type, Map<String, String> data) {
        try {
            restClient.post()
                    .uri("/internal/notifications")
                    .body(new NotificationRequest(userId, type, data))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // TODO: retry / outbox daca vrem garantie de livrare
            log.warn("Notificarea {} pentru user {} nu a putut fi trimisa: {}", type, userId, e.getMessage());
        }
    }
}
