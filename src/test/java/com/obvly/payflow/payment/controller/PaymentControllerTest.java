package com.obvly.payflow.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obvly.payflow.payment.exception.IdempotencyConflictException;
import com.obvly.payflow.payment.exception.InvalidPaymentStateException;
import com.obvly.payflow.payment.exception.PaymentNotFoundException;
import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePayment() throws Exception {
        Payment payment = new Payment(
                "ORD-200",
                new BigDecimal("99.99"),
                "EUR",
                "Controller test"
        );

        when(paymentService.createPayment(
                "ORD-200",
                new BigDecimal("99.99"),
                "EUR",
                "Controller test",
                "controller-test-200"
        )).thenReturn(payment);

        String request = """
                {
                  "orderId": "ORD-200",
                  "amount": 99.99,
                  "currency": "EUR",
                  "description": "Controller test"
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "controller-test-200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ORD-200"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldRejectInvalidPayment() throws Exception {
        String request = """
                {
                  "orderId": "",
                  "amount": -10,
                  "currency": "EU"
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "controller-test-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectRequestWithoutIdempotencyKey() throws Exception {
        String request = """
                {
                  "orderId": "ORD-302",
                  "amount": 20.00,
                  "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetPaymentById() throws Exception {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(
                "ORD-201",
                new BigDecimal("25.00"),
                "USD",
                "Get test"
        );

        when(paymentService.getPayment(id)).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-201"))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn404WhenPaymentDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(paymentService.getPayment(id))
                .thenThrow(new PaymentNotFoundException(id));

        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    void shouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldGetPaymentsWithPagination() throws Exception {
        Payment payment = new Payment(
                "ORD-202",
                new BigDecimal("10.00"),
                "EUR",
                null
        );

        when(paymentService.getPayments(any(), any()))
                .thenReturn(new PageImpl<>(
                        List.of(payment),
                        PageRequest.of(0, 10),
                        1
                ));

        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("ORD-202"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldStartPaymentProcessing() throws Exception {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(
                "ORD-500",
                new BigDecimal("50.00"),
                "EUR",
                null
        );
        payment.startProcessing();

        when(paymentService.startProcessing(id)).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/{id}/process", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void shouldCompletePayment() throws Exception {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(
                "ORD-501",
                new BigDecimal("50.00"),
                "EUR",
                null
        );
        payment.startProcessing();
        payment.complete();

        when(paymentService.complete(id)).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturn409ForInvalidPaymentTransition() throws Exception {
        UUID id = UUID.randomUUID();

        when(paymentService.complete(id))
                .thenThrow(new InvalidPaymentStateException(
                        "Payment can be completed only from PROCESSING status"
                ));

        mockMvc.perform(post("/api/v1/payments/{id}/complete", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAYMENT_STATE"));
    }

    @Test
    void shouldFailPayment() throws Exception {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(
                "ORD-502",
                new BigDecimal("50.00"),
                "EUR",
                null
        );
        payment.startProcessing();
        payment.fail();

        when(paymentService.fail(id)).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/{id}/fail", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void shouldCancelPayment() throws Exception {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(
                "ORD-600",
                new BigDecimal("30.00"),
                "EUR",
                null
        );
        payment.cancel();

        when(paymentService.cancel(id)).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn409WhenCancellingCompletedPayment() throws Exception {
        UUID id = UUID.randomUUID();

        when(paymentService.cancel(id))
                .thenThrow(new InvalidPaymentStateException(
                        "Payment can be cancelled only from PENDING status"
                ));

        mockMvc.perform(post("/api/v1/payments/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_PAYMENT_STATE"));
    }

    @Test
    void shouldRefundPayment() throws Exception {
        final UUID id = UUID.randomUUID();
        Payment payment = new Payment(
                "ORD-601",
                new BigDecimal("30.00"),
                "EUR",
                null
        );
        payment.startProcessing();
        payment.complete();
        payment.refund();

        when(paymentService.refund(id)).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/{id}/refund", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.001", "1.001", "100000000000000000.00"})
    void shouldRejectAmountsOutsideDatabasePrecision(String amount) throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "amount-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"ORD\",\"currency\":\"EUR\",\"amount\":"
                                + amount + "}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"eur", "123", "EU"})
    void shouldRejectInvalidCurrencyFormat(String currency) throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "currency-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"ORD\",\"amount\":1,\"currency\":\""
                                + currency + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankAndOversizedIdempotencyKeys() throws Exception {
        for (String key : List.of(" ", "x".repeat(101))) {
            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":\"ORD\",\"amount\":1,\"currency\":\"EUR\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void shouldReturn409ForConflictingIdempotencyKey() throws Exception {
        when(paymentService.createPayment(any(), any(), any(), any(), any()))
                .thenThrow(new IdempotencyConflictException());

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD","amount":1,"currency":"EUR","description":"test"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }
}
