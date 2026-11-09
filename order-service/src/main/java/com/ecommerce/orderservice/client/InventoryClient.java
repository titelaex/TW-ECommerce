package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.ClientDtos.ReservationRequest;
import com.ecommerce.orderservice.client.dto.ClientDtos.ReservationResponse;
import com.ecommerce.orderservice.exception.OutOfStockException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://inventory-service").build();
    }

    /**
     * Rezerva atomic toate produsele comenzii. inventory-service raspunde 409 daca
     * vreun produs nu mai are stoc suficient (aici se previne supra-vanzarea).
     */
    public ReservationResponse reserve(ReservationRequest request) {
        try {
            return restClient.post()
                    .uri("/internal/inventory/reservations")
                    .body(request)
                    .retrieve()
                    .body(ReservationResponse.class);
        } catch (HttpClientErrorException.Conflict e) {
            throw new OutOfStockException(e.getResponseBodyAsString());
        }
    }

    /** Dupa plata reusita: stocul rezervat devine definitiv scazut. */
    public void confirm(String reservationId) {
        restClient.post()
                .uri("/internal/inventory/reservations/{id}/confirm", reservationId)
                .retrieve()
                .toBodilessEntity();
    }

    /** Dupa plata esuata sau anulare: stocul revine disponibil. */
    public void release(String reservationId) {
        restClient.delete()
                .uri("/internal/inventory/reservations/{id}", reservationId)
                .retrieve()
                .toBodilessEntity();
    }
}
