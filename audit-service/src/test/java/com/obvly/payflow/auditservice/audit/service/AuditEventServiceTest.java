package com.obvly.payflow.auditservice.audit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.obvly.payflow.auditservice.audit.repository.AuditEventRepository;
import com.obvly.payflow.auditservice.payment.event.PaymentEvent;
import com.obvly.payflow.auditservice.payment.model.PaymentStatus;
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
        PaymentEvent event = new PaymentEvent(
                eventId,
                UUID.randomUUID(),
                "ORD-123",
                new BigDecimal("25.00"),
                "EUR",
                PaymentStatus.COMPLETED,
                Instant.now()
        );

        when(auditEventRepository.existsByEventId(eventId)).thenReturn(false);

        auditEventService.save(event);

        verify(auditEventRepository).save(any());
    }

    @Test
    void shouldIgnoreDuplicatePaymentEvent() {
        UUID eventId = UUID.randomUUID();
        PaymentEvent event = new PaymentEvent(
                eventId,
                UUID.randomUUID(),
                "ORD-123",
                new BigDecimal("25.00"),
                "EUR",
                PaymentStatus.COMPLETED,
                Instant.now()
        );

        when(auditEventRepository.existsByEventId(eventId)).thenReturn(true);

        auditEventService.save(event);

        verify(auditEventRepository, never()).save(any());
    }
}
