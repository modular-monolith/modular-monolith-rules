package com.example.notifications.internal;

import com.example.notifications.api.NotificationService;
import com.example.ordering.api.OrderPlacedEvent;

/**
 * Internal implementation of the notifications module's {@link NotificationService}.
 *
 * <p>Listens for domain events from other modules and sends appropriate notifications.
 */
public class NotificationServiceImpl implements NotificationService {

    // @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        sendOrderConfirmation(event.customerId(), event.orderId());
    }

    @Override
    public void sendOrderConfirmation(String customerId, String orderId) {
        System.out.println("Sending order confirmation to customer " + customerId
                + " for order " + orderId);
    }

    @Override
    public void sendShippingUpdate(String customerId, String orderId, String status) {
        System.out.println("Sending shipping update to customer " + customerId
                + " for order " + orderId + ": " + status);
    }
}
