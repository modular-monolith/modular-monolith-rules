package ${package}.ordering.api;

public interface OrderService {

    String placeOrder(String customerId, long amountInCents);
}
