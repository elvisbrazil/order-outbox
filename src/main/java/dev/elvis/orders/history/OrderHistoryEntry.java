package dev.elvis.orders.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_history")
public class OrderHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant occurredAt;

    protected OrderHistoryEntry() {}

    OrderHistoryEntry(UUID orderId, UUID eventId, String eventType, Instant occurredAt) {
        this.orderId = orderId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
