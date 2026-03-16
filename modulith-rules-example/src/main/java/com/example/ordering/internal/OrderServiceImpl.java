package com.example.ordering.internal;

import com.example.inventory.api.InventoryService;
import com.example.ordering.api.OrderDto;
import com.example.ordering.api.OrderPlacedEvent;
import com.example.ordering.api.OrderService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal implementation of the ordering module's {@link OrderService}.
 */
public class OrderServiceImpl implements OrderService {

    private final InventoryService inventoryService;
    // private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        // this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderDto placeOrder(String customerId, String productId, int quantity) {
        if (!inventoryService.checkAvailability(productId, quantity)) {
            throw new IllegalStateException(
                    "Insufficient stock for product " + productId + ", requested: " + quantity);
        }
        inventoryService.reserve(productId, quantity);

        String orderId = UUID.randomUUID().toString();
        BigDecimal totalPrice = BigDecimal.valueOf(quantity * 29.99);

        // eventPublisher.publishEvent(new OrderPlacedEvent(orderId, customerId, productId, quantity));

        return new OrderDto(orderId, customerId, productId, quantity, totalPrice, "PLACED");
    }

    @Override
    public OrderDto getOrder(String orderId) {
        return new OrderDto(orderId, "unknown", "unknown", 0, BigDecimal.ZERO, "UNKNOWN");
    }
}
