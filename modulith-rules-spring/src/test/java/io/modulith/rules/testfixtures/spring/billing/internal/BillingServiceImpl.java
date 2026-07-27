package io.modulith.rules.testfixtures.spring.billing.internal;

import io.modulith.rules.testfixtures.spring.billing.api.BillingService;
import io.modulith.rules.testfixtures.spring.payments.api.PaymentConfirmedEvent;
import io.modulith.rules.testfixtures.spring.payments.api.PaymentService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal implementation of BillingService that stays within the rules:
 *
 * <ul>
 *   <li>injects only its own module's repository plus {@link PaymentService},
 *       which is part of the payments public API
 *   <li>its @Transactional method only calls classes in the billing module
 *   <li>the cross-module call to {@link PaymentService#charge(String)} happens
 *       outside any transaction
 *   <li>listens to {@link PaymentConfirmedEvent}, which lives in payments.api
 * </ul>
 */
@Service
public class BillingServiceImpl implements BillingService {

    private final BillingRepository billingRepository;
    private final PaymentService paymentService;

    public BillingServiceImpl(BillingRepository billingRepository, PaymentService paymentService) {
        this.billingRepository = billingRepository;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public void issueInvoice(String orderId) {
        billingRepository.save(orderId);
    }

    /** Cross-module call through the public API, outside any @Transactional method. */
    public void collectPayment(String orderId) {
        paymentService.charge(orderId);
    }

    @EventListener
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
    }
}
