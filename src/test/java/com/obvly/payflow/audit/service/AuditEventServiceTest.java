package com.obvly.payflow.audit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(auditEventRepository.existsByEventId(eventId))
                .thenReturn(false);

        auditEventService.save(event);

        verify(auditEventRepository).save(argThat(auditEvent ->
                auditEvent.getEventId().equals(eventId)
                        && auditEvent.getPaymentId().equals(paymentId)
                        && auditEvent.getEventType()
                        .equals("PAYMENT_COMPLETED")
                        && auditEvent.getStatus().equals("COMPLETED")
        ));
    }

    @Test
    void shouldIgnoreDuplicatePaymentEvent() {
        UUID eventId = UUID.randomUUID();

        PaymentEvent event = new PaymentEvent(
                eventId, UUID.randomUUID(), "ORD-123", new BigDecimal("25.00"),
                "EUR", PaymentStatus.COMPLETED, Instant.now()
        );

        when(auditEventRepository.existsByEventId(eventId)).thenReturn(true);

        auditEventService.save(event);

        verify(auditEventRepository, never()).save(any());
    }
}
