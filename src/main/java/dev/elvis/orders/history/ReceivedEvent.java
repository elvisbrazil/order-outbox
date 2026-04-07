package dev.elvis.orders.history;

import java.time.Instant;
import java.util.UUID;

public record ReceivedEvent(UUID eventId, UUID orderId, String eventType, Instant occurredAt) {}
