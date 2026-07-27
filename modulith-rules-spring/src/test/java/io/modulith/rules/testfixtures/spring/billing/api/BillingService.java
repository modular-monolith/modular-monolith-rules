package io.modulith.rules.testfixtures.spring.billing.api;

/**
 * Public API interface for the billing test module.
 */
public interface BillingService {

    void issueInvoice(String orderId);
}
