package com.example.ordering.api;

/**
 * Public API for the ordering module.
 */
public interface OrderService {

    OrderDto placeOrder(String customerId, String productId, int quantity);

    OrderDto getOrder(String orderId);
}
