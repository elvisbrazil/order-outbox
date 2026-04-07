package dev.elvis.orders.history;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderHistoryProjector {

    private final OrderHistoryRepository history;
    private final ProcessedEventRepository processed;
    private final Clock clock;

    OrderHistoryProjector(OrderHistoryRepository history, ProcessedEventRepository processed, Clock clock) {
        this.history = history;
        this.processed = processed;
        this.clock = clock;
    }

    @Transactional
    public boolean apply(ReceivedEvent event) {
        if (processed.existsById(event.eventId())) {
            return false;
        }
        history.save(new OrderHistoryEntry(event.orderId(), event.eventId(), event.eventType(), event.occurredAt()));
        processed.save(new ProcessedEvent(event.eventId(), clock.instant()));
        return true;
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryEntry> historyOf(UUID orderId) {
        return history.findByOrderIdOrderByOccurredAt(orderId);
    }
}
