package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.domain.Payment;
import com.ecommerce.paymentservice.domain.PaymentStatus;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.FraudCheckService.FraudCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final FraudCheckService fraudCheckService;

    public PaymentService(PaymentRepository paymentRepository, FraudCheckService fraudCheckService) {
        this.paymentRepository = paymentRepository;
        this.fraudCheckService = fraudCheckService;
    }

    /** Fara @Transactional: daca insert-ul pierde la indexul unic, recitirea trebuie sa ruleze intr-o tranzactie noua. */
    public PaymentResponse processPayment(PaymentRequest req) {
        return paymentRepository.findByOrderId(req.orderId())
                .map(PaymentResponse::from)
                .orElseGet(() -> createPayment(req));
    }

    private PaymentResponse createPayment(PaymentRequest req) {
        Payment payment = new Payment(req.orderId(), req.userId(), req.amount(), req.currency());
        FraudCheckResult fraudCheck = fraudCheckService.evaluate(req);
        if (fraudCheck.isApproved()) {
            payment.markSucceeded("TXN-" + UUID.randomUUID());
        } else {
            payment.markFailed(fraudCheck.rejectionReason());
        }
        try {
            return PaymentResponse.from(paymentRepository.saveAndFlush(payment));
        } catch (DataIntegrityViolationException e) {
            // O alta cerere pentru aceeasi comanda a ajuns prima la indexul unic: returnam plata ei.
            return paymentRepository.findByOrderId(req.orderId())
                    .map(PaymentResponse::from)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public PaymentResponse refundPayment(UUID id, String reason) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
        if (payment.getStatus() != PaymentStatus.REFUNDED) {
            payment.markRefunded();
            log.info("Plata {} a fost rambursata. Motiv: {}", id, reason);
        }
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    public PaymentResponse getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    public List<PaymentResponse> getPaymentsByUserId(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentResponse::from)
                .toList();
    }
}
