package ${package}.payments.api;

public interface PaymentService {

    void charge(String orderId, long amountInCents);
}
