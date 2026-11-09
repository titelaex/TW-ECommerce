package com.ecommerce.paymentservice.web;

import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.exception.ForbiddenException;
import com.ecommerce.paymentservice.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final String ADMIN_ROLE = "ADMIN";

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id,
                                      @RequestHeader("X-User-Id") String userId,
                                      @RequestHeader("X-User-Role") String role) {
        PaymentResponse payment = paymentService.getPaymentById(id);
        if (!payment.userId().equals(userId) && !ADMIN_ROLE.equals(role)) {
            throw new ForbiddenException("Nu ai acces la plata " + id);
        }
        return payment;
    }

    @GetMapping("/my-history")
    public List<PaymentResponse> getMyPayments(@RequestHeader("X-User-Id") String userId) {
        return paymentService.getPaymentsByUserId(userId);
    }
}
