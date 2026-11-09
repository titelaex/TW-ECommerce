package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.service.FraudCheckService.FraudCheckResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FraudCheckServiceTest {

    private final FraudCheckService service = new FraudCheckService(new FraudProperties(
            new BigDecimal("50000"),
            Map.of("RON", BigDecimal.ONE, "EUR", new BigDecimal("5")),
            Set.of("blocked-user")));

    private FraudCheckResult evaluate(String amount, String currency, String userId) {
        return service.evaluate(new PaymentRequest("order-1", userId, new BigDecimal(amount), currency));
    }

    private void assertApproved(FraudCheckResult result) {
        assertTrue(result.isApproved());
        assertNull(result.rejectionReason());
    }

    private void assertRejected(FraudCheckResult result) {
        assertFalse(result.isApproved());
        assertFalse(result.rejectionReason() == null || result.rejectionReason().isBlank());
    }

    @Test
    void approvesNormalPayment() {
        assertApproved(evaluate("100.00", "RON", "user-1"));
    }

    @Test
    void rejectsZeroAmount() {
        assertRejected(evaluate("0.00", "RON", "user-1"));
    }

    @Test
    void rejectsNegativeAmount() {
        assertRejected(evaluate("-1.00", "RON", "user-1"));
    }

    @Test
    void approvesAmountExactlyAtLimit() {
        assertApproved(evaluate("50000.00", "RON", "user-1"));
    }

    @Test
    void rejectsAmountJustOverLimit() {
        assertRejected(evaluate("50000.01", "RON", "user-1"));
    }

    @Test
    void foreignCurrencyAtLimitInRonIsApproved() {
        assertApproved(evaluate("10000.00", "EUR", "user-1"));
    }

    @Test
    void foreignCurrencyOverLimitInRonIsRejected() {
        assertRejected(evaluate("10000.01", "EUR", "user-1"));
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertRejected(evaluate("100.00", "JPY", "user-1"));
    }

    @Test
    void rejectsBlockedUser() {
        assertRejected(evaluate("100.00", "RON", "blocked-user"));
    }
}
