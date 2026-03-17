package dev.elvis.orders.order;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Order %s not found".formatted(orderId));
    }
}
