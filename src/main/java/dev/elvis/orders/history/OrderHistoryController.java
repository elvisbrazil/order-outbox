package dev.elvis.orders.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OrderHistoryController {

    private final OrderHistoryProjector projector;

    OrderHistoryController(OrderHistoryProjector projector) {
        this.projector = projector;
    }

    @GetMapping("/orders/{id}/history")
    List<HistoryItem> history(@PathVariable UUID id) {
        return projector.historyOf(id).stream()
                .map(entry -> new HistoryItem(entry.getEventType(), entry.getOccurredAt()))
                .toList();
    }

    record HistoryItem(String eventType, Instant occurredAt) {}
}
