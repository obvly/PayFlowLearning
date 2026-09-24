package com.obvly.payflow.payment.repository;

import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.model.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
