package io.gabrielsilva117.ht_payment_ms.entity;

import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentEntityStatus;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Uniqueness is conditional (at most one non-FAILED row per cart_id), enforced by
    // the partial index uk_payments_cart_active (see V3 migration) - not expressible
    // as a plain @Column(unique = true).
    @Column(name = "cart_id", nullable = false, updatable = false)
    private UUID cartId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20, updatable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentEntityStatus status;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "card_last_4", length = 4)
    private String cardLast4;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = PaymentEntityStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Builds the initial PENDING row from an inbound DTO. The DTO never carries raw card
     * data (card is already tokenized client-side) - cardLast4 is populated later, from
     * Stripe, once the gateway call resolves.
     */
    public static Payment pendingFrom(PaymentDTO dto) {
        Payment payment = new Payment();
        payment.setCartId(dto.getCartId());
        payment.setAmount(dto.getAmount());
        payment.setCurrency(dto.getCurrency());
        payment.setPaymentType(dto.getType());
        payment.setStatus(PaymentEntityStatus.PENDING);
        return payment;
    }
}
