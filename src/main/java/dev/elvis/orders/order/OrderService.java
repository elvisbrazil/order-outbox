package dev.elvis.orders.order;

import dev.elvis.orders.order.OrderEvent.OrderCreated;
import dev.elvis.orders.outbox.OutboxWriter;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OutboxWriter outbox;
    private final Clock clock;

    public OrderService(OrderRepository orders, OutboxWriter outbox, Clock clock) {
        this.orders = orders;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public Order create(String customerId, BigDecimal amount) {
        var order = orders.save(Order.create(customerId, amount, clock.instant()));
        record(new OrderCreated(order.getId(), order.getCustomerId(), order.getAmount(), order.getCreatedAt()));
        return order;
    }

    @Transactional(readOnly = true)
    public Order find(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void record(OrderEvent event) {
        outbox.append("Order", event.orderId().toString(), event.type(), event);
    }
}
