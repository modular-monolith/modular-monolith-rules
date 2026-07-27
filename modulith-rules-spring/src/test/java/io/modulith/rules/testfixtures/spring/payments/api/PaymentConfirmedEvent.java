package io.modulith.rules.testfixtures.spring.payments.api;

/**
 * Event published by the payments module through its public API package.
 *
 * <p>Because it lives in payments.api, other modules may listen to it without
 * violating {@code eventClassesShouldBeInApiPackages}.
 */
public class PaymentConfirmedEvent {

    private final String orderId;

    public PaymentConfirmedEvent(String orderId) {
        this.orderId = orderId;
    }

    public String orderId() {
        return orderId;
    }
}
