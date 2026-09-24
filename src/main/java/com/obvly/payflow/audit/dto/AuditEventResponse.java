package com.obvly.payflow.audit.dto;

import com.obvly.payflow.audit.model.AuditEvent;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID eventId,
        UUID paymentId,
        String eventType,
        String status,
        Instant occurredAt
) {

    public static AuditEventResponse from(AuditEvent auditEvent) {
        return new AuditEventResponse(
                auditEvent.getId(),
                auditEvent.getEventId(),
                auditEvent.getPaymentId(),
                auditEvent.getEventType(),
                auditEvent.getStatus(),
                auditEvent.getOccurredAt()
        );
    }
}
