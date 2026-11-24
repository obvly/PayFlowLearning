package com.obvly.payflow.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.obvly.payflow.audit.repository.AuditEventRepository;
import com.obvly.payflow.audit.service.AuditEventService;
import com.obvly.payflow.payment.event.PaymentEvent;
import com.obvly.payflow.payment.event.PaymentEventPublisher;
import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.model.PaymentStatus;
import com.obvly.payflow.payment.repository.PaymentRepository;
import com.obvly.payflow.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "spring.cache.type=none",
        "payflow.audit.consumer.enabled=false"
})
class PaymentIdempotencyIntegrationTest {

    @Container
    private static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("payflow")
                    .withUsername("payflow")
                    .withPassword("payflow");

    @MockBean
    private PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        auditEventRepository.deleteAll();
    }

    @Test
    void shouldReturnSamePaymentForSameIdempotencyKey() {
        String idempotencyKey = "integration-key-001";

        Payment first = paymentService.createPayment(
                "ORD-TEST-001",
                new BigDecimal("19.99"),
                "EUR",
                "Integration test",
                idempotencyKey
        );

        Payment second = paymentService.createPayment(
                "ORD-TEST-001",
                new BigDecimal("19.99"),
                "EUR",
                "Integration test",
                idempotencyKey
        );

        assertThat(second.getId()).isEqualTo(first.getId());

        assertThat(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .hasValueSatisfying(payment ->
                        assertThat(payment.getId()).isEqualTo(first.getId()));

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnSamePaymentForConcurrentRequests() throws Exception {
        String idempotencyKey = "integration-concurrent-key-001";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier start = new CyclicBarrier(2);

        try {
            CompletableFuture<Payment> first = CompletableFuture.supplyAsync(
                    () -> createPaymentAfter(start, idempotencyKey), executor);
            CompletableFuture<Payment> second = CompletableFuture.supplyAsync(
                    () -> createPaymentAfter(start, idempotencyKey), executor);

            List<Payment> payments = CompletableFuture.allOf(first, second)
                    .thenApply(ignored -> List.of(first.join(), second.join()))
                    .get(30, TimeUnit.SECONDS);

            UUID paymentId = payments.getFirst().getId();
            assertThat(payments).allSatisfy(payment ->
                    assertThat(payment.getId()).isEqualTo(paymentId));
            assertThat(paymentRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Payment createPayment(String idempotencyKey) {
        return paymentService.createPayment(
                "ORD-CONCURRENT-001", new BigDecimal("19.99"), "EUR",
                "Concurrent integration test", idempotencyKey);
    }

    private Payment createPaymentAfter(
            CyclicBarrier start, String idempotencyKey) {
        try {
            start.await(30, TimeUnit.SECONDS);
            return createPayment(idempotencyKey);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void shouldIgnoreConcurrentDuplicateAuditEvents() throws Exception {
        PaymentEvent event = new PaymentEvent(
                UUID.randomUUID(), UUID.randomUUID(), "ORD-AUDIT-CONCURRENT",
                new BigDecimal("25.00"), "EUR", PaymentStatus.COMPLETED,
                java.time.Instant.now());
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> saveAuditAfter(start, event));
            var second = executor.submit(() -> saveAuditAfter(start, event));
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);

            assertThat(auditEventRepository.findAll())
                    .singleElement()
                    .extracting(auditEvent -> auditEvent.getEventId())
                    .isEqualTo(event.eventId());
        } finally {
            executor.shutdownNow();
        }
    }

    private void saveAuditAfter(CyclicBarrier start, PaymentEvent event) {
        try {
            start.await(30, TimeUnit.SECONDS);
            auditEventService.save(event);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
