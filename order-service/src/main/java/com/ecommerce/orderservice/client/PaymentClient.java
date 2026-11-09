package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.ClientDtos.PaymentRequest;
import com.ecommerce.orderservice.client.dto.ClientDtos.PaymentResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://payment-service").build();
    }

    public PaymentResponse pay(PaymentRequest request) {
        return restClient.post()
                .uri("/internal/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    // TODO: refund(paymentId) pentru anularea unei comenzi deja CONFIRMED
}
