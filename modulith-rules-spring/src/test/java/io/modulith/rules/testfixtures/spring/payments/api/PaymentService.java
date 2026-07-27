package io.modulith.rules.testfixtures.spring.payments.api;

/**
 * Public API interface for the payments test module.
 */
public interface PaymentService {

    void charge(String orderId);
}
