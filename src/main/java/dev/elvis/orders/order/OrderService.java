package dev.elvis.orders.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final Clock clock;

    public OrderService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Transactional
    public Order create(String customerId, BigDecimal amount) {
        return orders.save(Order.create(customerId, amount, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Order find(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
