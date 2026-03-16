package com.example.ordering.api;

import java.math.BigDecimal;

/**
 * Data transfer object representing an order.
 */
public record OrderDto(
        String orderId,
        String customerId,
        String productId,
        int quantity,
        BigDecimal totalPrice,
        String status
) {
}
