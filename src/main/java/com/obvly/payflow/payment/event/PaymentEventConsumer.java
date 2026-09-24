package com.obvly.payflow.payment.event;

import com.obvly.payflow.audit.service.AuditEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "payflow.audit.consumer.enabled",
        havingValue = "true",
        matchIfMissing = true
)
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
