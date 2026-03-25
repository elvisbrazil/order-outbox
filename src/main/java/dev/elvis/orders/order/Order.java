package dev.elvis.orders.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Order() {}

    private Order(UUID id, String customerId, BigDecimal amount, Instant now) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.status = OrderStatus.CREATED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Order create(String customerId, BigDecimal amount, Instant now) {
        return new Order(UUID.randomUUID(), customerId, amount, now);
    }

    public void pay(Instant now) {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot pay order " + id + " in status " + status);
        }
        moveTo(OrderStatus.PAID, now);
    }

    public void cancel(Instant now) {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order " + id + " is already cancelled");
        }
        moveTo(OrderStatus.CANCELLED, now);
    }

    private void moveTo(OrderStatus next, Instant now) {
        this.status = next;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
