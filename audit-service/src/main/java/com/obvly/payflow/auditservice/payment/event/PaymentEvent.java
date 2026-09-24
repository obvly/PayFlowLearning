package com.obvly.payflow.auditservice.payment.event;

import com.obvly.payflow.auditservice.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
        UUID eventId,
        UUID paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant occurredAt
) {
}
