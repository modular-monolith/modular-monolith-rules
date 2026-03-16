package com.example.inventory.api;

/**
 * Public API for the inventory module.
 */
public interface InventoryService {

    boolean checkAvailability(String productId, int quantity);

    void reserve(String productId, int quantity);

    int getStockLevel(String productId);
}
