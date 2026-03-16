package com.example.ordering.api;

/**
 * Event published when an order is placed successfully.
 *
 * <p>This event is placed in the API package because other modules (such as
 * notifications) consume it. By being part of the public API, downstream modules
 * can depend on it without accessing the ordering module's internals.
 */
public record OrderPlacedEvent(
        String orderId,
        String customerId,
        String productId,
        int quantity
) {
}
