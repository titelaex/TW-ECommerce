package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.ClientDtos.CartResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CartClient {

    private final RestClient restClient;

    public CartClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://cart-service").build();
    }

    public CartResponse getCart(String userId) {
        return restClient.get()
                .uri("/internal/carts/{userId}", userId)
                .retrieve()
                .body(CartResponse.class);
    }

    public void clearCart(String userId) {
        restClient.delete()
                .uri("/internal/carts/{userId}", userId)
                .retrieve()
                .toBodilessEntity();
    }
}
