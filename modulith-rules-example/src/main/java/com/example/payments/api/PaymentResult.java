package com.example.payments.api;

/**
 * Result of a payment processing operation.
 */
public record PaymentResult(
        String paymentId,
        String orderId,
        String status
) {
}
