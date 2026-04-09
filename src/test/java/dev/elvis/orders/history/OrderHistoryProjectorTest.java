package dev.elvis.orders.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderHistoryProjectorTest {

    private static final Instant NOW = Instant.parse("2026-04-06T22:00:00Z");

    private final OrderHistoryRepository history = mock(OrderHistoryRepository.class);
    private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
    private final OrderHistoryProjector projector =
            new OrderHistoryProjector(history, processed, Clock.fixed(NOW, ZoneOffset.UTC));

    private final ReceivedEvent event =
            new ReceivedEvent(UUID.randomUUID(), UUID.randomUUID(), "OrderPaid", NOW.minusSeconds(5));

    @Test
    void recordsNewEventAndRemembersItsId() {
        when(processed.existsById(event.eventId())).thenReturn(false);

        var applied = projector.apply(event);

        assertThat(applied).isTrue();
        var entry = ArgumentCaptor.forClass(OrderHistoryEntry.class);
        verify(history).save(entry.capture());
        assertThat(entry.getValue().getOrderId()).isEqualTo(event.orderId());
        assertThat(entry.getValue().getEventType()).isEqualTo("OrderPaid");
        verify(processed).save(any(ProcessedEvent.class));
    }

    @Test
    void ignoresRedeliveredEvent() {
        when(processed.existsById(event.eventId())).thenReturn(true);

        var applied = projector.apply(event);

        assertThat(applied).isFalse();
        verify(history, never()).save(any());
        verify(processed, never()).save(any());
    }
}
