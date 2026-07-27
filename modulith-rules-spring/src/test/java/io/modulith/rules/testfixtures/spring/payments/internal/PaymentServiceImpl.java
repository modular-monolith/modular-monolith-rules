package io.modulith.rules.testfixtures.spring.payments.internal;

import io.modulith.rules.testfixtures.spring.payments.api.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal implementation of PaymentService. Its @Transactional method only calls
 * classes within the payments module, which is the clean arrangement for
 * {@code transactionalMethodsShouldNotSpanModules}.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentLedgerRepository ledger;

    public PaymentServiceImpl(PaymentLedgerRepository ledger) {
        this.ledger = ledger;
    }

    @Override
    @Transactional
    public void charge(String orderId) {
        ledger.record(orderId);
    }
}
