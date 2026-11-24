package com.obvly.payflow.auditservice.audit.repository;

import com.obvly.payflow.auditservice.audit.model.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO audit_events (
                id, event_id, payment_id, event_type, status, occurred_at
            ) VALUES (
                :id, :eventId, :paymentId, :eventType, :status, :occurredAt
            ) ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("paymentId") UUID paymentId,
            @Param("eventType") String eventType,
            @Param("status") String status,
            @Param("occurredAt") java.time.Instant occurredAt
    );

    boolean existsByEventId(UUID eventId);

    List<AuditEvent> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);

    List<AuditEvent> findByPaymentIdAndStatusOrderByOccurredAtAsc(
            UUID paymentId, String status);

    Page<AuditEvent> findByStatus(String status, Pageable pageable);
}
