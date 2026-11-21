package io.gabrielsilva117.ht_payment_ms.exception;

import java.util.UUID;

public class DuplicatePaymentException extends RuntimeException {
    private final UUID cartId;

    public DuplicatePaymentException(UUID cartId, Throwable cause) {
        super("Duplicate payment detected for cartId " + cartId, cause);
        this.cartId = cartId;
    }

    public UUID getCartId() {
        return cartId;
    }
}
