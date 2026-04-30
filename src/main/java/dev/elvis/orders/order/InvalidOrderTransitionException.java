package dev.elvis.orders.order;

import java.util.UUID;

public class InvalidOrderTransitionException extends RuntimeException {

    public InvalidOrderTransitionException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("Order %s cannot go from %s to %s".formatted(orderId, from, to));
    }
}
