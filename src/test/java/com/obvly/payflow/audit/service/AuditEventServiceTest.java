package com.obvly.payflow.audit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.obvly.payflow.audit.repository.AuditEventRepository;
import com.obvly.payflow.payment.event.PaymentEvent;
import com.obvly.payflow.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditEventService auditEventService;

    @Test
    void shouldSaveNewPaymentEvent() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentEvent event = new PaymentEvent(
                eventId, paymentId, "ORD-123", new BigDecimal("25.00"),
                "EUR", PaymentStatus.COMPLETED, Instant.now()
        );

        auditEventService.save(event);

        verify(auditEventRepository).insertIfAbsent(
                any(), eq(eventId), eq(paymentId), eq("PAYMENT_COMPLETED"),
                eq("COMPLETED"), eq(event.occurredAt()));
    }

    @Test
    void shouldIgnoreDuplicatePaymentEvent() {
        UUID eventId = UUID.randomUUID();

        PaymentEvent event = new PaymentEvent(
                eventId, UUID.randomUUID(), "ORD-123", new BigDecimal("25.00"),
                "EUR", PaymentStatus.COMPLETED, Instant.now()
        );

        auditEventService.save(event);

        verify(auditEventRepository).insertIfAbsent(
                any(), eq(eventId), any(), any(), any(), eq(event.occurredAt()));
    }
}
