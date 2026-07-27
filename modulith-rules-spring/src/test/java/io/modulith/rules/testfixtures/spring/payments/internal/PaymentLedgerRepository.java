package io.modulith.rules.testfixtures.spring.payments.internal;

import org.springframework.stereotype.Repository;

/**
 * Internal repository of the payments test module.
 *
 * <p>Accessing this class from another module violates
 * {@code repositoriesShouldBeModuleInternal}.
 */
@Repository
public class PaymentLedgerRepository {

    public void record(String orderId) {
    }
}
