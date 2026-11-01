package com.ecommerce.apigateway;

import com.ecommerce.apigateway.security.AuthenticatedUser;
import com.ecommerce.apigateway.security.TokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Porneste gateway-ul complet, cu verificarea Firebase inlocuita de un mock (nu e nevoie de cheie). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.lazy-initialization=true")
@AutoConfigureWebTestClient
class AuthMeEndpointTest {

    @Autowired
    WebTestClient client;

    @MockitoBean
    TokenVerifier verifier;

    @Test
    void meReturnsUserFromToken() throws Exception {
        when(verifier.verify(anyString())).thenReturn(new AuthenticatedUser("uid-1", "a@b.ro", "SELLER"));

        client.get().uri("/api/auth/me").header("Authorization", "Bearer x")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.uid").isEqualTo("uid-1")
                .jsonPath("$.role").isEqualTo("SELLER");
    }

    @Test
    void meWithoutTokenIs401() {
        client.get().uri("/api/auth/me").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
