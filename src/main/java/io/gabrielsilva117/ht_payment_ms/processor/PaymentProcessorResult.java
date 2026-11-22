package io.gabrielsilva117.ht_payment_ms.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentProcessorResult {

    public enum Outcome {
        SUCCESS,
        FAILURE,
        PENDING
    }

    private final Outcome outcome;
    private final String gatewayReference;
    private final String failureReason;
    private final String cardLast4;

    public static PaymentProcessorResult success(String gatewayReference) {
        return new PaymentProcessorResult(Outcome.SUCCESS, gatewayReference, null, null);
    }

    public static PaymentProcessorResult failure(String failureReason) {
        return new PaymentProcessorResult(Outcome.FAILURE, null, failureReason, null);
    }

    public static PaymentProcessorResult pending(String gatewayReference) {
        return pending(gatewayReference, null);
    }

    public static PaymentProcessorResult pending(String gatewayReference, String cardLast4) {
        return new PaymentProcessorResult(Outcome.PENDING, gatewayReference, null, cardLast4);
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }

    public boolean isPending() {
        return outcome == Outcome.PENDING;
    }
}
