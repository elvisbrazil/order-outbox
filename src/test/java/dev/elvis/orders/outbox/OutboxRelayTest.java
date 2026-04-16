package dev.elvis.orders.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-03-28T17:00:00Z");

    private final OutboxRepository repository = mock(OutboxRepository.class);

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private final OutboxRelay relay = new OutboxRelay(
            repository,
            kafka,
            new OutboxProperties("orders.events", 10, Duration.ofSeconds(1)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void publishesBatchAndMarksEventsAsPublished() {
        var first = event("order-1", "OrderCreated");
        var second = event("order-2", "OrderCreated");
        when(repository.lockNextBatch(10)).thenReturn(List.of(first, second));
        doReturn(CompletableFuture.completedFuture(null)).when(kafka).send(any(ProducerRecord.class));

        var published = relay.publishPending();

        assertThat(published).isEqualTo(2);
        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(second.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesAggregateIdAsKeyAndSendsEventMetadataInHeaders() {
        var event = event("order-1", "OrderPaid");
        when(repository.lockNextBatch(10)).thenReturn(List.of(event));
        doReturn(CompletableFuture.completedFuture(null)).when(kafka).send(any(ProducerRecord.class));

        relay.publishPending();

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(captor.capture());
        var record = captor.getValue();
        assertThat(record.topic()).isEqualTo("orders.events");
        assertThat(record.key()).isEqualTo("order-1");
        assertThat(record.value()).isEqualTo(event.getPayload());
        assertThat(header(record, OutboxRelay.EVENT_ID_HEADER))
                .isEqualTo(event.getId().toString());
        assertThat(header(record, OutboxRelay.EVENT_TYPE_HEADER)).isEqualTo("OrderPaid");
    }

    @Test
    void stopsAtFirstFailureAndLeavesRemainingEventsUnpublished() {
        var first = event("order-1", "OrderCreated");
        var failing = event("order-1", "OrderPaid");
        var third = event("order-1", "OrderCancelled");
        when(repository.lockNextBatch(10)).thenReturn(List.of(first, failing, third));
        doReturn(CompletableFuture.completedFuture(null))
                .doReturn(CompletableFuture.failedFuture(new KafkaException("broker unavailable")))
                .when(kafka)
                .send(any(ProducerRecord.class));

        var published = relay.publishPending();

        assertThat(published).isEqualTo(1);
        assertThat(first.isPublished()).isTrue();
        assertThat(failing.isPublished()).isFalse();
        assertThat(third.isPublished()).isFalse();
        verify(kafka, times(2)).send(any(ProducerRecord.class));
    }

    @Test
    void treatsSynchronousSendErrorAsFailure() {
        var event = event("order-1", "OrderCreated");
        when(repository.lockNextBatch(10)).thenReturn(List.of(event));
        when(kafka.send(any(ProducerRecord.class))).thenThrow(new KafkaException("not connected"));

        assertThat(relay.publishPending()).isZero();
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    void doesNothingWhenThereIsNothingPending() {
        when(repository.lockNextBatch(10)).thenReturn(List.of());

        assertThat(relay.publishPending()).isZero();
    }

    private static OutboxEvent event(String aggregateId, String type) {
        return new OutboxEvent("Order", aggregateId, type, "{\"orderId\":\"" + aggregateId + "\"}", NOW);
    }

    private static String header(ProducerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
