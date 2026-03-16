package com.example.inventory.internal;

import com.example.inventory.api.InventoryService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal implementation of the inventory module's {@link InventoryService}.
 */
public class InventoryServiceImpl implements InventoryService {

    private final ConcurrentHashMap<String, Integer> stockLevels = new ConcurrentHashMap<>();

    public InventoryServiceImpl() {
        stockLevels.put("PROD-001", 100);
        stockLevels.put("PROD-002", 50);
        stockLevels.put("PROD-003", 0);
    }

    @Override
    public boolean checkAvailability(String productId, int quantity) {
        return stockLevels.getOrDefault(productId, 0) >= quantity;
    }

    @Override
    public void reserve(String productId, int quantity) {
        stockLevels.compute(productId, (id, current) -> {
            int available = current == null ? 0 : current;
            if (available < quantity) {
                throw new IllegalStateException(
                        "Insufficient stock for product " + productId
                        + ": available=" + available + ", requested=" + quantity);
            }
            return available - quantity;
        });
    }

    @Override
    public int getStockLevel(String productId) {
        return stockLevels.getOrDefault(productId, 0);
    }
}
