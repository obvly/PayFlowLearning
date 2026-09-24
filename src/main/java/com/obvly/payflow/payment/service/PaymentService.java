package com.obvly.payflow.payment.service;

import com.obvly.payflow.payment.event.PaymentEventPublisher;
import com.obvly.payflow.payment.exception.IdempotencyConflictException;
import com.obvly.payflow.payment.exception.PaymentNotFoundException;
import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.model.PaymentStatus;
import com.obvly.payflow.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventPublisher paymentEventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "#id")
    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional
    public Payment createPayment(
            String orderId,
            BigDecimal amount,
            String currency,
            String description,
            String idempotencyKey
    ) {
        if (idempotencyKey != null) {
            Optional<Payment> existingPayment = paymentRepository
                    .findByIdempotencyKey(idempotencyKey);

            if (existingPayment.isPresent()) {
                Payment existing = existingPayment.get();
                if (!Objects.equals(existing.getOrderId(), orderId)
                        || existing.getAmount().compareTo(amount) != 0
                        || !Objects.equals(existing.getCurrency(), currency)
                        || !Objects.equals(existing.getDescription(), description)) {
                    throw new IdempotencyConflictException();
                }
                return existing;
            }
        }

        Payment payment = new Payment(
                orderId,
                amount,
                currency,
                description,
                idempotencyKey
        );

        Payment savedPayment = paymentRepository.save(payment);
        paymentEventPublisher.publish(savedPayment);
        return savedPayment;
    }

    @Transactional
    public Payment createPayment(
            String orderId,
            BigDecimal amount,
            String currency,
            String description
    ) {
        return createPayment(orderId, amount, currency, description, null);
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPayments(
            PaymentStatus status,
            Pageable pageable
    ) {
        if (status == null) {
            return paymentRepository.findAll(pageable);
        }

        return paymentRepository.findByStatus(status, pageable);
    }

    @CachePut(value = "payments", key = "#result.id")
    @Transactional
    public Payment startProcessing(UUID id) {
        Payment payment = getPayment(id);
        payment.startProcessing();
        paymentEventPublisher.publish(payment);
        return payment;
    }

    @CachePut(value = "payments", key = "#result.id")
    @Transactional
    public Payment complete(UUID id) {
        Payment payment = getPayment(id);
        payment.complete();
        paymentEventPublisher.publish(payment);
        return payment;
    }

    @CachePut(value = "payments", key = "#result.id")
    @Transactional
    public Payment fail(UUID id) {
        Payment payment = getPayment(id);
        payment.fail();
        paymentEventPublisher.publish(payment);
        return payment;
    }

    @CachePut(value = "payments", key = "#result.id")
    @Transactional
    public Payment cancel(UUID id) {
        Payment payment = getPayment(id);
        payment.cancel();
        paymentEventPublisher.publish(payment);
        return payment;
    }

    @CachePut(value = "payments", key = "#result.id")
    @Transactional
    public Payment refund(UUID id) {
        Payment payment = getPayment(id);
        payment.refund();
        paymentEventPublisher.publish(payment);
        return payment;
    }
}
