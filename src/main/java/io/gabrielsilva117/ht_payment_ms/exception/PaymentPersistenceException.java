package io.gabrielsilva117.ht_payment_ms.exception;

public class PaymentPersistenceException extends RuntimeException {
    public PaymentPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
