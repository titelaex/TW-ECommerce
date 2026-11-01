package com.ecommerce.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param credentialsPath calea catre JSON-ul de service account (secret, nu se urca in git)
 * @param projectId       id-ul proiectului Firebase
 */
@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(String credentialsPath, String projectId) {
}
