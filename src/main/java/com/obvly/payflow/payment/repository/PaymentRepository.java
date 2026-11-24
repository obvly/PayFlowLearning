package com.obvly.payflow.payment.repository;

import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.model.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO payments (
                id, order_id, amount, currency, description, idempotency_key,
                status, created_at, updated_at
            ) VALUES (
                :id, :orderId, :amount, :currency, :description, :idempotencyKey,
                'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            ) ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("orderId") String orderId,
            @Param("amount") java.math.BigDecimal amount,
            @Param("currency") String currency,
            @Param("description") String description,
            @Param("idempotencyKey") String idempotencyKey
    );

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
