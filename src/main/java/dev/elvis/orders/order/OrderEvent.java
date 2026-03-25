package dev.elvis.orders.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public sealed interface OrderEvent {

    UUID orderId();

    Instant occurredAt();

    default String type() {
        return switch (this) {
            case OrderCreated ignored -> "OrderCreated";
            case OrderPaid ignored -> "OrderPaid";
            case OrderCancelled ignored -> "OrderCancelled";
        };
    }

    record OrderCreated(UUID orderId, String customerId, BigDecimal amount, Instant occurredAt) implements OrderEvent {}

    record OrderPaid(UUID orderId, BigDecimal amount, Instant occurredAt) implements OrderEvent {}

    record OrderCancelled(UUID orderId, OrderStatus previousStatus, Instant occurredAt) implements OrderEvent {}
}
