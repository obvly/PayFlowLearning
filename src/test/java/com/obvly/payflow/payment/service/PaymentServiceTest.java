package com.obvly.payflow.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.obvly.payflow.payment.event.PaymentEventPublisher;
import com.obvly.payflow.payment.exception.IdempotencyConflictException;
import com.obvly.payflow.payment.exception.InvalidPaymentStateException;
import com.obvly.payflow.payment.exception.PaymentNotFoundException;
import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.model.PaymentStatus;
import com.obvly.payflow.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentWithPendingStatus() {
        Payment payment = new Payment(
                "ORD-100",
                new BigDecimal("49.99"),
                "EUR",
                "Test payment"
        );

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);

        Payment result = paymentService.createPayment(
                "ORD-100",
                new BigDecimal("49.99"),
                "EUR",
                "Test payment"
        );

        assertThat(result.getStatus()).isEqualTo(
                PaymentStatus.PENDING);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldReturnPaymentById() {
        UUID id = UUID.randomUUID();

        Payment payment = new Payment(
                "ORD-101",
                new BigDecimal("20.00"),
                "USD",
                null
        );

        when(paymentRepository.findById(id))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.getPayment(id);

        assertThat(result).isSameAs(payment);
        verify(paymentRepository).findById(id);
    }

    @Test
    void shouldThrowExceptionWhenPaymentDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(paymentRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(id))
                .isInstanceOf(PaymentNotFoundException.class);

        verify(paymentRepository).findById(id);
    }

    @Test
    void shouldReturnExistingPaymentForSameIdempotencyKey() {
        String idempotencyKey = "payment-key-001";

        Payment existingPayment = new Payment(
                "ORD-300",
                new BigDecimal("75.00"),
                "EUR",
                "Existing payment",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingPayment));

        Payment result = paymentService.createPayment(
                "ORD-300",
                new BigDecimal("75.00"),
                "EUR",
                "Existing payment",
                idempotencyKey
        );

        assertThat(result).isSameAs(existingPayment);

        verify(paymentRepository).findByIdempotencyKey(idempotencyKey);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldCreatePaymentWhenIdempotencyKeyIsNew() {
        String idempotencyKey = "payment-key-002";

        Payment payment = new Payment(
                "ORD-301",
                new BigDecimal("15.00"),
                "USD",
                "New payment",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);

        Payment result = paymentService.createPayment(
                "ORD-301",
                new BigDecimal("15.00"),
                "USD",
                "New payment",
                idempotencyKey
        );

        assertThat(result).isSameAs(payment);

        verify(paymentRepository).findByIdempotencyKey(idempotencyKey);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldStartProcessingPayment() {
        UUID id = UUID.randomUUID();

        Payment payment = new Payment(
                "ORD-400",
                new BigDecimal("30.00"),
                "EUR",
                null
        );

        when(paymentRepository.findById(id))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.startProcessing(id);

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void shouldCompleteProcessingPayment() {
        UUID id = UUID.randomUUID();

        Payment payment = new Payment(
                "ORD-401",
                new BigDecimal("30.00"),
                "EUR",
                null
        );

        when(paymentRepository.findById(id))
                .thenReturn(Optional.of(payment));

        payment.startProcessing();

        Payment result = paymentService.complete(id);

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void shouldRejectCompletionFromPendingStatus() {
        UUID id = UUID.randomUUID();

        Payment payment = new Payment(
                "ORD-402",
                new BigDecimal("30.00"),
                "EUR",
                null
        );

        when(paymentRepository.findById(id))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.complete(id))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void shouldCancelPendingPayment() {
        UUID id = UUID.randomUUID();

        Payment payment = new Payment(
                "ORD-600",
                new BigDecimal("30.00"),
                "EUR",
                null
        );

        when(paymentRepository.findById(id))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.cancel(id);

        assertThat(result.getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void shouldRejectCancellationOfCompletedPayment() {
        UUID id = UUID.randomUUID();

        Payment payment = new Payment(
                "ORD-601",
                new BigDecimal("30.00"),
                "EUR",
                null
        );

        when(paymentRepository.findById(id))
                .thenReturn(Optional.of(payment));

        payment.startProcessing();
        payment.complete();

        assertThatThrownBy(() -> paymentService.cancel(id))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRefundCompletedPayment() {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(
                "ORD-602",
                new BigDecimal("30.00"),
                "EUR",
                null
        );
        payment.startProcessing();
        payment.complete();

        when(paymentRepository.findById(id))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.refund(id);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @ParameterizedTest
    @CsvSource({
        "OTHER, 75.00, EUR, Existing payment",
        "ORD-300, 76.00, EUR, Existing payment",
        "ORD-300, 75.00, USD, Existing payment",
        "ORD-300, 75.00, EUR, Changed description"
    })
    void shouldRejectKeyReuseWithDifferentPayload(
            String orderId, BigDecimal amount, String currency, String description) {
        Payment existing = new Payment(
                "ORD-300", new BigDecimal("75.00"), "EUR", "Existing payment", "key");
        when(paymentRepository.findByIdempotencyKey("key")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.createPayment(
                orderId, amount, currency, description, "key"))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(paymentRepository, never()).save(any());
        verify(paymentEventPublisher, never()).publish(any());
    }

    @Test
    void shouldTreatNumericallyEqualAmountsAsTheSameRequest() {
        Payment existing = new Payment("ORD", new BigDecimal("1.00"), "EUR", null, "key");
        when(paymentRepository.findByIdempotencyKey("key")).thenReturn(Optional.of(existing));

        assertThat(paymentService.createPayment("ORD", BigDecimal.ONE, "EUR", null, "key"))
                .isSameAs(existing);
        verify(paymentEventPublisher, never()).publish(any());
    }
}
