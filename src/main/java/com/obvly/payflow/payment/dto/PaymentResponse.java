package com.obvly.payflow.payment.dto;

import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String orderId,
        BigDecimal amount,
        String currency,
        String description,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
