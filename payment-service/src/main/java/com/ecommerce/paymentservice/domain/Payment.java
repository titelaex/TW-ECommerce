package com.ecommerce.paymentservice.domain;

import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "uq_payments_order_id", columnList = "orderId", unique = true),
        @Index(name = "idx_payments_user_created", columnList = "userId, createdAt")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String orderId;

    /** Firebase UID al cumparatorului. */
    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private String failureReason;

    private String transactionReference;

    @Version
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(String orderId, String userId, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
    }

    public void markSucceeded(String txnRef) {
        requireStatus(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED);
        status = PaymentStatus.SUCCEEDED;
        transactionReference = txnRef;
    }

    public void markFailed(String reason) {
        requireStatus(PaymentStatus.PENDING, PaymentStatus.FAILED);
        status = PaymentStatus.FAILED;
        failureReason = reason;
    }

    public void markRefunded() {
        requireStatus(PaymentStatus.SUCCEEDED, PaymentStatus.REFUNDED);
        status = PaymentStatus.REFUNDED;
    }

    private void requireStatus(PaymentStatus required, PaymentStatus next) {
        if (status != required) {
            throw new InvalidPaymentStateException(id, status, next);
        }
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getTransactionReference() { return transactionReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
