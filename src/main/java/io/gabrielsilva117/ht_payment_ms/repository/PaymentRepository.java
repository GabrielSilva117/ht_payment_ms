package io.gabrielsilva117.ht_payment_ms.repository;

import io.gabrielsilva117.ht_payment_ms.entity.Payment;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentEntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByCartId(UUID cartId);
    Optional<Payment> findByGatewayReference(String gatewayReference);

    List<Payment> findByStatusInAndCreatedAtBefore(List<PaymentEntityStatus> statuses, Instant cutoff);

    // Conditional on the row still being PENDING/PROCESSING at update time, atomically -
    // a plain load-mutate-save would risk clobbering a payment that the Stripe webhook
    // flips to SUCCEEDED in the same instant the timeout sweep tries to fail it.
    @Modifying
    @Query("""
            UPDATE Payment p
            SET p.status = 'FAILED', p.failureReason = :reason, p.updatedAt = :now, p.version = p.version + 1
            WHERE p.id = :id AND p.status IN ('PENDING', 'PROCESSING')
            """)
    int failIfStillPending(@Param("id") UUID id, @Param("reason") String reason, @Param("now") Instant now);

    // Mirrors failIfStillPending for the success side - same atomicity reason: the real
    // webhook, a duplicate webhook redelivery, and the timeout sweep's reconciliation can
    // all try to resolve the same row around the same time, and exactly one must win.
    @Modifying
    @Query("""
            UPDATE Payment p
            SET p.status = 'SUCCEEDED', p.gatewayReference = :gatewayReference, p.updatedAt = :now, p.version = p.version + 1
            WHERE p.id = :id AND p.status IN ('PENDING', 'PROCESSING')
            """)
    int succeedIfStillLive(@Param("id") UUID id, @Param("gatewayReference") String gatewayReference, @Param("now") Instant now);
}
