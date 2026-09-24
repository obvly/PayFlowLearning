package com.obvly.payflow.payment.model;

import com.obvly.payflow.payment.exception.InvalidPaymentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "payments")
public class Payment implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(String orderId, BigDecimal amount,
                   String currency, String description) {
        this(orderId, amount, currency, description, null);
    }

    public Payment(String orderId, BigDecimal amount,
                   String currency, String description,
                   String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void startProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(
                    "Payment can start processing only from PENDING status"
            );
        }

        this.status = PaymentStatus.PROCESSING;
    }

    public void complete() {
        if (status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStateException(
                    "Payment can be completed only from PROCESSING status"
            );
        }

        this.status = PaymentStatus.COMPLETED;
    }

    public void fail() {
        if (status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStateException(
                    "Payment can fail only from PROCESSING status"
            );
        }

        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(
                    "Payment can be cancelled only from PENDING status"
            );
        }

        this.status = PaymentStatus.CANCELLED;
    }

    public void refund() {
        if (status != PaymentStatus.COMPLETED) {
            throw new InvalidPaymentStateException(
                    "Payment can be refunded only from COMPLETED status"
            );
        }

        this.status = PaymentStatus.REFUNDED;
    }
}
