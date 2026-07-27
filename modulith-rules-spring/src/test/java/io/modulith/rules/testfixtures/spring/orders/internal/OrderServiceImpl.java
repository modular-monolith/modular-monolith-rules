package io.modulith.rules.testfixtures.spring.orders.internal;

import io.modulith.rules.testfixtures.spring.orders.api.OrderService;
import io.modulith.rules.testfixtures.spring.payments.api.PaymentService;
import io.modulith.rules.testfixtures.spring.payments.internal.PaymentLedgerRepository;
import io.modulith.rules.testfixtures.spring.payments.internal.PaymentSettledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal implementation of OrderService that intentionally violates several
 * Spring-specific rules:
 *
 * <ul>
 *   <li>injects {@link PaymentLedgerRepository}, an internal repository of the
 *       payments module (violates repositoriesShouldBeModuleInternal and
 *       noDirectInjectionOfInternalBeans)
 *   <li>calls {@link PaymentService#charge(String)} from a @Transactional method
 *       (violates transactionalMethodsShouldNotSpanModules)
 *   <li>listens to {@link PaymentSettledEvent}, which lives in payments.internal
 *       rather than payments.api (violates eventClassesShouldBeInApiPackages)
 * </ul>
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final PaymentService paymentService;

    /** Internal repository of another module - this injection is the violation under test. */
    private final PaymentLedgerRepository paymentLedger;

    public OrderServiceImpl(PaymentService paymentService, PaymentLedgerRepository paymentLedger) {
        this.paymentService = paymentService;
        this.paymentLedger = paymentLedger;
    }

    /** Transactional method calling into the payments module - the violation under test. */
    @Override
    @Transactional
    public void placeOrder(String orderId) {
        paymentService.charge(orderId);
    }

    /** Listens to an event from another module's internal package - the violation under test. */
    @EventListener
    public void onPaymentSettled(PaymentSettledEvent event) {
    }
}
