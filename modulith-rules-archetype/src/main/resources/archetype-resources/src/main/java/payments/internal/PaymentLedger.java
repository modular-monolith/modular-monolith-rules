package ${package}.payments.internal;

import ${package}.payments.api.PaymentService;

import java.util.ArrayList;
import java.util.List;

public class PaymentLedger implements PaymentService {

    private final List<String> entries = new ArrayList<>();

    @Override
    public void charge(String orderId, long amountInCents) {
        entries.add(orderId + ":" + amountInCents);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
