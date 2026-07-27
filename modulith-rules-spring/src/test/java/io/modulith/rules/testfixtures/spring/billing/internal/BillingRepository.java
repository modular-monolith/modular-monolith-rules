package io.modulith.rules.testfixtures.spring.billing.internal;

import org.springframework.stereotype.Repository;

/**
 * Internal repository of the billing test module. Only accessed from within the
 * billing module, which is the clean arrangement for
 * {@code repositoriesShouldBeModuleInternal}.
 */
@Repository
public class BillingRepository {

    public void save(String invoiceId) {
    }
}
