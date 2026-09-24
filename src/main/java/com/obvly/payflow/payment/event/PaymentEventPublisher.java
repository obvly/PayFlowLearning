package com.obvly.payflow.payment.event;

import com.obvly.payflow.payment.model.Payment;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentEventPublisher(
            KafkaTemplate<String, PaymentEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Payment payment) {
        PaymentEvent event = new PaymentEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                Instant.now()
        );

        kafkaTemplate.send(
                TOPIC,
                payment.getId().toString(),
                event
        );
    }
}
