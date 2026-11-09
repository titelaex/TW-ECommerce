package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FraudCheckService {

    public record FraudCheckResult(boolean isApproved, String rejectionReason) {

        private static FraudCheckResult approved() {
            return new FraudCheckResult(true, null);
        }

        private static FraudCheckResult rejected(String reason) {
            return new FraudCheckResult(false, reason);
        }
    }

    private final FraudProperties properties;

    public FraudCheckService(FraudProperties properties) {
        this.properties = properties;
    }

    public FraudCheckResult evaluate(PaymentRequest req) {
        if (req.amount().signum() <= 0) {
            return FraudCheckResult.rejected("Suma trebuie sa fie pozitiva");
        }
        BigDecimal ronRate = properties.ronRates().get(req.currency());
        if (ronRate == null) {
            return FraudCheckResult.rejected("Moneda neacceptata: " + req.currency());
        }
        if (req.amount().multiply(ronRate).compareTo(properties.maxAmountRon()) > 0) {
            return FraudCheckResult.rejected("Suma depaseste limita de " + properties.maxAmountRon().toPlainString() + " RON");
        }
        if (properties.blockedUsers().contains(req.userId())) {
            return FraudCheckResult.rejected("Cont blocat");
        }
        return FraudCheckResult.approved();
    }
}
