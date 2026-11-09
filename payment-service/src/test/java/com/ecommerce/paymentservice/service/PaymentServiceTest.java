package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.domain.Payment;
import com.ecommerce.paymentservice.domain.PaymentStatus;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PaymentServiceTest {

    private static final FraudCheckService FRAUD = new FraudCheckService(new FraudProperties(
            new BigDecimal("50000"), Map.of("RON", BigDecimal.ONE), Set.of("blocked-user")));

    @Autowired
    private PaymentRepository repository;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(repository, FRAUD);
    }

    private PaymentRequest request(String orderId, String userId) {
        return new PaymentRequest(orderId, userId, new BigDecimal("100.00"), "RON");
    }

    private PaymentStatus storedStatus(UUID id) {
        return repository.findById(id).orElseThrow().getStatus();
    }

    @Test
    void approvedPaymentIsSavedAsSucceededWithTransactionReference() {
        PaymentResponse response = service.processPayment(request("order-1", "user-1"));

        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        assertNotNull(response.paymentId());
        assertTrue(response.txnRef().startsWith("TXN-"));
        assertNull(response.failureReason());
        assertEquals(PaymentStatus.SUCCEEDED, storedStatus(response.paymentId()));
    }

    @Test
    void eachApprovedPaymentGetsItsOwnTransactionReference() {
        String first = service.processPayment(request("order-1", "user-1")).txnRef();
        String second = service.processPayment(request("order-2", "user-1")).txnRef();

        assertFalse(first.equals(second));
    }

    @Test
    void rejectedPaymentIsSavedAsFailedWithReason() {
        PaymentResponse response = service.processPayment(request("order-1", "blocked-user"));

        assertEquals(PaymentStatus.FAILED, response.status());
        assertNull(response.txnRef());
        assertFalse(response.failureReason().isBlank());
        assertEquals(PaymentStatus.FAILED, storedStatus(response.paymentId()));
    }

    @Test
    void repeatedCallForSameOrderReturnsSamePaymentWithoutSecondRow() {
        PaymentResponse first = service.processPayment(request("order-1", "user-1"));
        PaymentResponse second = service.processPayment(request("order-1", "user-1"));

        assertEquals(first.paymentId(), second.paymentId());
        assertEquals(first.txnRef(), second.txnRef());
        assertEquals(1, repository.count());
    }

    @Test
    void failedOrderStaysFailedWhenCalledAgain() {
        PaymentResponse first = service.processPayment(request("order-1", "blocked-user"));
        PaymentResponse second = service.processPayment(request("order-1", "user-1"));

        assertEquals(first.paymentId(), second.paymentId());
        assertEquals(PaymentStatus.FAILED, second.status());
        assertEquals(1, repository.count());
    }

    /** Doua cereri simultane: a doua pierde la indexul unic si trebuie sa primeasca plata primeia. */
    @Test
    void losingConcurrentInsertReturnsTheWinningPayment() {
        PaymentRepository racingRepository = mock(PaymentRepository.class);
        Payment winner = new Payment("order-1", "user-1", new BigDecimal("100.00"), "RON");
        winner.markSucceeded("TXN-winner");
        when(racingRepository.findByOrderId("order-1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(racingRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("uq_payments_order_id"));

        PaymentResponse response = new PaymentService(racingRepository, FRAUD)
                .processPayment(request("order-1", "user-1"));

        assertEquals("TXN-winner", response.txnRef());
    }

    @Test
    void refundMovesSucceededPaymentToRefunded() {
        UUID id = service.processPayment(request("order-1", "user-1")).paymentId();

        PaymentResponse response = service.refundPayment(id, "comanda anulata");

        assertEquals(PaymentStatus.REFUNDED, response.status());
        assertEquals(PaymentStatus.REFUNDED, storedStatus(id));
    }

    @Test
    void secondRefundReturnsTheRefundedPayment() {
        UUID id = service.processPayment(request("order-1", "user-1")).paymentId();
        service.refundPayment(id, "comanda anulata");

        PaymentResponse response = service.refundPayment(id, "comanda anulata");

        assertEquals(PaymentStatus.REFUNDED, response.status());
    }

    @Test
    void refundOfFailedPaymentIsRejected() {
        UUID id = service.processPayment(request("order-1", "blocked-user")).paymentId();

        assertThrows(InvalidPaymentStateException.class, () -> service.refundPayment(id, "comanda anulata"));
        assertEquals(PaymentStatus.FAILED, storedStatus(id));
    }

    @Test
    void refundOfUnknownPaymentIsNotFound() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThrows(PaymentNotFoundException.class, () -> service.refundPayment(unknown, "comanda anulata"));
    }

    @Test
    void getPaymentByIdReturnsStoredPayment() {
        UUID id = service.processPayment(request("order-1", "user-1")).paymentId();

        assertEquals("order-1", service.getPaymentById(id).orderId());
    }

    @Test
    void getPaymentByIdOfUnknownPaymentIsNotFound() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThrows(PaymentNotFoundException.class, () -> service.getPaymentById(unknown));
    }

    @Test
    void getPaymentByOrderIdReturnsThatOrdersPayment() {
        UUID id = service.processPayment(request("order-1", "user-1")).paymentId();
        service.processPayment(request("order-2", "user-1"));

        assertEquals(id, service.getPaymentByOrderId("order-1").paymentId());
    }

    @Test
    void getPaymentByOrderIdOfUnknownOrderIsNotFound() {
        assertThrows(PaymentNotFoundException.class, () -> service.getPaymentByOrderId("order-404"));
    }

    @Test
    void getPaymentsByUserIdReturnsOnlyThatUsersPaymentsNewestFirst() {
        service.processPayment(request("order-1", "user-1"));
        service.processPayment(request("order-2", "user-2"));
        service.processPayment(request("order-3", "user-1"));

        List<String> orderIds = service.getPaymentsByUserId("user-1").stream()
                .map(PaymentResponse::orderId)
                .toList();

        assertEquals(List.of("order-3", "order-1"), orderIds);
    }
}
