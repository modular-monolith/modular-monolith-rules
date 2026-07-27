package io.modulith.rules.testfixtures.spring.orders.web;

import io.modulith.rules.testfixtures.spring.orders.api.OrderService;
import io.modulith.rules.testfixtures.spring.payments.web.PaymentController;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the orders test module. Intentionally depends on
 * {@link PaymentController}, a controller in a different module, which violates
 * controllersShouldNotCrossModuleBoundaries.
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    /** Controller from another module - this dependency is the violation under test. */
    private final PaymentController paymentController;

    public OrderController(OrderService orderService, PaymentController paymentController) {
        this.orderService = orderService;
        this.paymentController = paymentController;
    }

    public void placeOrder(String orderId) {
        orderService.placeOrder(orderId);
        paymentController.pay(orderId);
    }
}
