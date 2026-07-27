package io.modulith.rules.testfixtures.spring.payments.internal;

/**
 * Event that lives in the internal package of the payments test module.
 *
 * <p>When another module references it, {@code eventClassesShouldBeInApiPackages}
 * reports a violation because the event is not part of the payments public API.
 */
public class PaymentSettledEvent {

    private final String orderId;

    public PaymentSettledEvent(String orderId) {
        this.orderId = orderId;
    }

    public String orderId() {
        return orderId;
    }
}
