package com.obvly.payflow.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.obvly.payflow.payment.event.PaymentEventPublisher;
import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.repository.PaymentRepository;
import com.obvly.payflow.payment.service.PaymentService;
import java.math.BigDecimal;
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

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
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
}
