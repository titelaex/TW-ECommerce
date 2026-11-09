package com.ecommerce.paymentservice.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * @param maxAmountRon limita unei plati, exprimata in RON
 * @param ronRates     cati RON face o unitate din fiecare moneda acceptata
 * @param blockedUsers Firebase UID-urile conturilor blocate
 */
@ConfigurationProperties(prefix = "payment.fraud")
public record FraudProperties(BigDecimal maxAmountRon, Map<String, BigDecimal> ronRates, Set<String> blockedUsers) {
}
