package io.modulith.rules.testfixtures.spring.billing.web;

import io.modulith.rules.testfixtures.spring.billing.api.BillingService;
import io.modulith.rules.testfixtures.spring.payments.api.PaymentService;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the billing test module. Depends on its own module's API and
 * on {@link PaymentService} from the payments public API, but never on another
 * module's controller - the clean arrangement for
 * {@code controllersShouldNotCrossModuleBoundaries}.
 */
@RestController
public class BillingController {

    private final BillingService billingService;
    private final PaymentService paymentService;

    public BillingController(BillingService billingService, PaymentService paymentService) {
        this.billingService = billingService;
        this.paymentService = paymentService;
    }

    public void invoice(String orderId) {
        billingService.issueInvoice(orderId);
    }
}
