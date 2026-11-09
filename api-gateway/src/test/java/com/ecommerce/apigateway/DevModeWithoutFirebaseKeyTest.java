package com.ecommerce.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Fara mock si fara cheie Firebase: in mod dev gateway-ul trebuie sa porneasca si sa accepte doar dev-token. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "firebase.dev-mode=true")
@AutoConfigureWebTestClient
class DevModeWithoutFirebaseKeyTest {

    @Autowired
    WebTestClient client;

    @Test
    void devTokenIdentifiesUser() {
        client.get().uri("/api/auth/me").header("Authorization", "Bearer dev-token:user-7:SELLER")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.uid").isEqualTo("user-7")
                .jsonPath("$.role").isEqualTo("SELLER");
    }

    @Test
    void regularTokenIsRejected() {
        client.get().uri("/api/auth/me").header("Authorization", "Bearer not-a-dev-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
