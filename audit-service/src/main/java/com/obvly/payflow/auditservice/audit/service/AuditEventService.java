package com.obvly.payflow.auditservice.audit.service;

import com.obvly.payflow.auditservice.audit.model.AuditEvent;
import com.obvly.payflow.auditservice.audit.repository.AuditEventRepository;
import com.obvly.payflow.auditservice.payment.event.PaymentEvent;
import com.obvly.payflow.auditservice.payment.model.PaymentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void save(PaymentEvent paymentEvent) {
        if (auditEventRepository.existsByEventId(paymentEvent.eventId())) {
            return;
        }

        AuditEvent auditEvent = new AuditEvent(
                paymentEvent.eventId(),
                paymentEvent.paymentId(),
                "PAYMENT_" + paymentEvent.status().name(),
                paymentEvent.status().name(),
                paymentEvent.occurredAt()
        );

        auditEventRepository.save(auditEvent);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> findByPaymentId(UUID paymentId) {
        return auditEventRepository.findByPaymentIdOrderByOccurredAtAsc(paymentId);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> findByPaymentIdAndStatus(
            UUID paymentId, String status) {
        String normalizedStatus = status.toUpperCase();
        PaymentStatus.valueOf(normalizedStatus);
        return auditEventRepository.findByPaymentIdAndStatusOrderByOccurredAtAsc(
                paymentId, normalizedStatus);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findAll(Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> findByStatus(String status, Pageable pageable) {
        String normalizedStatus = status.toUpperCase();
        PaymentStatus.valueOf(normalizedStatus);
        return auditEventRepository.findByStatus(normalizedStatus, pageable);
    }
}
