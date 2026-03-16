package com.example.payments.api;

import java.math.BigDecimal;

/**
 * Public API for the payments module.
 */
public interface PaymentService {

    PaymentResult processPayment(String orderId, BigDecimal amount);
}
