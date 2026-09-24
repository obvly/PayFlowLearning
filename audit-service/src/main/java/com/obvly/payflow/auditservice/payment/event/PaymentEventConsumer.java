package com.obvly.payflow.auditservice.payment.event;

import com.obvly.payflow.auditservice.audit.service.AuditEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final String TOPIC = "payment-events";
    private static final String GROUP_ID = "payflow-audit-service";

    private final AuditEventService auditEventService;

    public PaymentEventConsumer(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public void consume(PaymentEvent paymentEvent) {
        auditEventService.save(paymentEvent);
    }
}
