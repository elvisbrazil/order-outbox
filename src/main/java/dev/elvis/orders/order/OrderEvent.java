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
        };
    }

    record OrderCreated(UUID orderId, String customerId, BigDecimal amount, Instant occurredAt) implements OrderEvent {}
}
