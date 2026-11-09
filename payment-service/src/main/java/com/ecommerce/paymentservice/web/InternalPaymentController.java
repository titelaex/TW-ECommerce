package com.ecommerce.paymentservice.web;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Apelat doar de order-service. O plata refuzata raspunde tot 200, cu status FAILED in corp. */
@RestController
@RequestMapping("/internal/payments")
public class InternalPaymentController {

    private final PaymentService paymentService;

    public InternalPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse processPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return paymentService.getPaymentById(id);
    }

    @GetMapping("/orders/{orderId}")
    public PaymentResponse getPaymentByOrder(@PathVariable String orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return paymentService.refundPayment(id, request.reason());
    }
}
