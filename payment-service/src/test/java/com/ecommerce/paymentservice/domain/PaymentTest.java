package com.ecommerce.paymentservice.domain;

import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    private Payment newPayment() {
        return new Payment("order-1", "user-1", new BigDecimal("100.00"), "RON");
    }

    @Test
    void newPaymentIsPending() {
        assertEquals(PaymentStatus.PENDING, newPayment().getStatus());
    }

    @Test
    void markSucceededRecordsTransactionReference() {
        Payment payment = newPayment();
        payment.markSucceeded("TXN-1");
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        assertEquals("TXN-1", payment.getTransactionReference());
    }

    @Test
    void markFailedRecordsReason() {
        Payment payment = newPayment();
        payment.markFailed("Cont blocat");
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("Cont blocat", payment.getFailureReason());
    }

    @Test
    void succeededPaymentCanBeRefunded() {
        Payment payment = newPayment();
        payment.markSucceeded("TXN-1");
        payment.markRefunded();
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void failedPaymentCannotBeRefunded() {
        Payment payment = newPayment();
        payment.markFailed("Cont blocat");
        assertThrows(InvalidPaymentStateException.class, payment::markRefunded);
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void pendingPaymentCannotBeRefunded() {
        Payment payment = newPayment();
        assertThrows(InvalidPaymentStateException.class, payment::markRefunded);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void finishedPaymentCannotBeMarkedAgain() {
        Payment payment = newPayment();
        payment.markSucceeded("TXN-1");
        assertThrows(InvalidPaymentStateException.class, () -> payment.markFailed("prea tarziu"));
        assertThrows(InvalidPaymentStateException.class, () -> payment.markSucceeded("TXN-2"));
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        assertEquals("TXN-1", payment.getTransactionReference());
    }
}
