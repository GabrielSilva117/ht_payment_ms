package io.gabrielsilva117.ht_payment_ms.service;

import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.entity.Payment;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentEntityStatus;
import io.gabrielsilva117.ht_payment_ms.exception.DuplicatePaymentException;
import io.gabrielsilva117.ht_payment_ms.exception.PaymentPersistenceException;
import io.gabrielsilva117.ht_payment_ms.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentPersistenceService {
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPending(PaymentDTO dto) {
        Payment payment = Payment.pendingFrom(dto);
        try {
            // flush is required here: with GenerationType.UUID, Hibernate defers the actual
            // INSERT to transaction commit, which happens after this try/catch returns - a
            // real cart_id collision would otherwise throw uncaught, past this method's callers.
            // uk_payments_cart_active only rejects this if a non-FAILED row already exists for
            // the cart (another live attempt, or an already-SUCCEEDED one) - a prior FAILED
            // attempt does not block a new one.
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePaymentException(dto.getCartId(), e);
        } catch (DataAccessException e) {
            throw new PaymentPersistenceException(
                    "Failed to persist payment for cartId " + dto.getCartId(), e);
        }
    }

    @Transactional
    public Payment markProcessing(UUID paymentId, String gatewayReference) {
        return markProcessing(paymentId, gatewayReference, null);
    }

    @Transactional
    public Payment markProcessing(UUID paymentId, String gatewayReference, String cardLast4) {
        Payment payment = getOrThrow(paymentId);
        payment.setStatus(PaymentEntityStatus.PROCESSING);
        payment.setGatewayReference(gatewayReference);
        if (cardLast4 != null) {
            payment.setCardLast4(cardLast4);
        }
        return payment;
    }

    @Transactional
    public Payment markSucceeded(UUID paymentId, String gatewayReference) {
        Payment payment = getOrThrow(paymentId);
        payment.setStatus(PaymentEntityStatus.SUCCEEDED);
        payment.setGatewayReference(gatewayReference);
        return payment;
    }

    @Transactional
    public Payment markFailed(UUID paymentId, String failureReason) {
        Payment payment = getOrThrow(paymentId);
        payment.setStatus(PaymentEntityStatus.FAILED);
        payment.setFailureReason(failureReason);
        return payment;
    }

    /**
     * Used by the timeout sweep for a row with no gateway_reference (PENDING - the gateway
     * call itself never completed, nothing to reconcile). Unlike {@link #markFailed}, this
     * only applies if the row is still PENDING/PROCESSING at the moment of the update - if
     * something else resolved it concurrently, this is a no-op.
     *
     * @return true if the row was actually failed by this call
     */
    @Transactional
    public boolean timeoutFail(UUID paymentId, String failureReason) {
        return paymentRepository.failIfStillPending(paymentId, failureReason, Instant.now()) > 0;
    }

    /**
     * Atomic success resolution, used by {@link PaymentOutcomeResolver} - applies only if the
     * row is still PENDING/PROCESSING. The real webhook, a duplicate webhook redelivery, and
     * the timeout sweep's reconciliation can all attempt this around the same time; exactly
     * one wins.
     *
     * @return true if this call actually resolved the row to SUCCEEDED
     */
    @Transactional
    public boolean succeedIfStillLive(UUID paymentId, String gatewayReference) {
        return paymentRepository.succeedIfStillLive(paymentId, gatewayReference, Instant.now()) > 0;
    }

    /**
     * Atomic failure resolution, used by {@link PaymentOutcomeResolver} - same guarantee as
     * {@link #succeedIfStillLive}, just resolving to FAILED instead.
     *
     * @return true if this call actually resolved the row to FAILED
     */
    @Transactional
    public boolean failIfStillLive(UUID paymentId, String failureReason) {
        return paymentRepository.failIfStillPending(paymentId, failureReason, Instant.now()) > 0;
    }

    @Transactional(readOnly = true)
    public List<Payment> findStale(List<PaymentEntityStatus> statuses, Instant cutoff) {
        return paymentRepository.findByStatusInAndCreatedAtBefore(statuses, cutoff);
    }

    @Transactional(readOnly = true)
    public Payment findByCartId(UUID cartId) {
        return paymentRepository.findByCartId(cartId)
                .orElseThrow(() -> new EntityNotFoundException("No payment for cartId " + cartId));
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByGatewayReference(String gatewayReference) {
        return paymentRepository.findByGatewayReference(gatewayReference);
    }

    private Payment getOrThrow(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("No payment with id " + paymentId));
    }
}
