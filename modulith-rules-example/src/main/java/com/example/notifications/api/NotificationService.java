package com.example.notifications.api;

/**
 * Public API for the notifications module.
 */
public interface NotificationService {

    void sendOrderConfirmation(String customerId, String orderId);

    void sendShippingUpdate(String customerId, String orderId, String status);
}
