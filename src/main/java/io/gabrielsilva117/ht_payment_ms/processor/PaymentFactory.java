package io.gabrielsilva117.ht_payment_ms.processor;

import io.gabrielsilva117.ht_payment_ms.enums.paymentType;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentFactory {
    // Gets all active processor beans
    private final Map<PaymentType, PaymentProcessor> processors;

    public PaymentFactory(List<PaymentProcessor> processorList) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(
                        PaymentProcessor::getSupportedType,
                        Function.identity()
                ));
    }

    public PaymentProcessor getProcessor(PaymentType paymentType) {
        PaymentProcessor processor = processors.get(paymentType);

        if (processor == null) {
            throw new IllegalArgumentException("No processor found for card type: " + paymentType);
        }

        return processor;
    }
}
