package com.obvly.payflow.auditservice.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.obvly.payflow.auditservice.audit.repository.AuditEventRepository;
import com.obvly.payflow.auditservice.audit.service.AuditEventService;
import com.obvly.payflow.auditservice.payment.event.PaymentEvent;
import com.obvly.payflow.auditservice.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class AuditEventConcurrencyIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("payflow")
                    .withUsername("payflow")
                    .withPassword("payflow");

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void shouldIgnoreConcurrentDuplicateEvents() throws Exception {
        UUID eventId = UUID.randomUUID();
        PaymentEvent event = new PaymentEvent(
                eventId, UUID.randomUUID(), "ORD-AUDIT-CONCURRENT",
                new BigDecimal("25.00"), "EUR", PaymentStatus.COMPLETED,
                Instant.now());
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> saveAfter(start, event));
            var second = executor.submit(() -> saveAfter(start, event));

            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);

            assertThat(auditEventRepository.findAll())
                    .singleElement()
                    .extracting(auditEvent -> auditEvent.getEventId())
                    .isEqualTo(eventId);
        } finally {
            executor.shutdownNow();
        }
    }

    private void saveAfter(CyclicBarrier start, PaymentEvent event) {
        try {
            start.await(30, TimeUnit.SECONDS);
            auditEventService.save(event);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
