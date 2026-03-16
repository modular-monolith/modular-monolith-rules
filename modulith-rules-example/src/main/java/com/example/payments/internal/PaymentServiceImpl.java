package com.example.payments.internal;

import com.example.payments.api.PaymentResult;
import com.example.payments.api.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal implementation of the payments module's {@link PaymentService}.
 */
public class PaymentServiceImpl implements PaymentService {

    @Override
    public PaymentResult processPayment(String orderId, BigDecimal amount) {
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        return new PaymentResult(paymentId, orderId, "COMPLETED");
    }
}
