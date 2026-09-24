package com.obvly.payflow.audit.repository;

import com.obvly.payflow.audit.model.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    boolean existsByEventId(UUID eventId);

    List<AuditEvent> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);

    List<AuditEvent> findByPaymentIdAndStatusOrderByOccurredAtAsc(
            UUID paymentId, String status);

    Page<AuditEvent> findByStatus(String status, Pageable pageable);
}
