package io.modulith.rules.testfixtures.spring.payments.web;

import io.modulith.rules.testfixtures.spring.payments.api.PaymentService;
import org.springframework.stereotype.Controller;

/**
 * Controller of the payments test module. Depends only on classes in its own
 * module, so it never causes a controller boundary violation by itself.
 */
@Controller
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void pay(String orderId) {
        paymentService.charge(orderId);
    }
}
