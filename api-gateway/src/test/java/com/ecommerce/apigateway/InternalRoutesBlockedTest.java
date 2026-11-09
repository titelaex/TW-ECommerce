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

/** Gateway complet: /internal nu are ruta, iar blocarea trebuie sa ruleze inaintea autentificarii. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.lazy-initialization=true")
@AutoConfigureWebTestClient
class InternalRoutesBlockedTest {

    @Autowired
    WebTestClient client;

    @MockitoBean
    TokenVerifier verifier;

    @Test
    void internalPathWithoutTokenIs403() {
        client.get().uri("/internal/payments").exchange().expectStatus().isForbidden();
    }

    @Test
    void internalPathWithValidTokenIs403() throws Exception {
        when(verifier.verify(anyString())).thenReturn(new AuthenticatedUser("uid-1", "a@b.ro", "ADMIN"));

        client.get().uri("/internal/payments").header("Authorization", "Bearer x")
                .exchange()
                .expectStatus().isForbidden();
    }
}
