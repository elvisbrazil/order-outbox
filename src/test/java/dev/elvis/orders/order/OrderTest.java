package dev.elvis.orders.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-03-14T18:00:00Z");

    private final Order order = Order.create("customer-1", new BigDecimal("49.90"), NOW);

    @Test
    void startsAsCreated() {
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void paysCreatedOrder() {
        var later = NOW.plusSeconds(30);

        order.pay(later);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void rejectsPayingTwice() {
        order.pay(NOW);

        assertThatThrownBy(() -> order.pay(NOW)).isInstanceOf(InvalidOrderTransitionException.class);
    }

    @Test
    void cancelsPaidOrder() {
        order.pay(NOW);

        order.cancel(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void rejectsPayingCancelledOrder() {
        order.cancel(NOW);

        assertThatThrownBy(() -> order.pay(NOW)).isInstanceOf(InvalidOrderTransitionException.class);
    }

    @Test
    void rejectsCancellingTwice() {
        order.cancel(NOW);

        assertThatThrownBy(() -> order.cancel(NOW)).isInstanceOf(InvalidOrderTransitionException.class);
    }
}
