package ${package}.ordering.internal;

import ${package}.ordering.api.OrderService;
import ${package}.payments.api.PaymentService;

import java.util.UUID;

/**
 * Ordering reaches Payments only through its api package. Replace the import
 * above with {@code ${package}.payments.internal.PaymentLedger} to watch the
 * architecture test fail and explain itself.
 */
public class OrderServiceImpl implements OrderService {

    private final PaymentService paymentService;

    public OrderServiceImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public String placeOrder(String customerId, long amountInCents) {
        String orderId = UUID.randomUUID().toString();
        paymentService.charge(orderId, amountInCents);
        return orderId;
    }
}
