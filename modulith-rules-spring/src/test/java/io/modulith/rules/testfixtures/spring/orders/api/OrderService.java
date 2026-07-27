package io.modulith.rules.testfixtures.spring.orders.api;

/**
 * Public API interface for the orders test module.
 */
public interface OrderService {

    void placeOrder(String orderId);
}
