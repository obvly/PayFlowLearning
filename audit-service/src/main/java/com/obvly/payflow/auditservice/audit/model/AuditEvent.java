package com.obvly.payflow.auditservice.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID eventId, UUID paymentId, String eventType,
                      String status, Instant occurredAt) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStatus() {
        return status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
