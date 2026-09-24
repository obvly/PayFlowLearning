package com.obvly.payflow.payment.event;

import static org.mockito.Mockito.verify;

import com.obvly.payflow.audit.service.AuditEventService;
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
class PaymentEventConsumerTest {

    @Mock
    private AuditEventService auditEventService;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    void shouldForwardPaymentEventToAuditService() {
        PaymentEvent event = new PaymentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORD-123",
                new BigDecimal("10.00"),
                "EUR",
                PaymentStatus.COMPLETED,
                Instant.now()
        );

        paymentEventConsumer.consume(event);

        verify(auditEventService).save(event);
    }
}
