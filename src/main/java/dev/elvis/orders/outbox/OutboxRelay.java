package dev.elvis.orders.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    public static final String EVENT_ID_HEADER = "eventId";
    public static final String EVENT_TYPE_HEADER = "eventType";

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxRelay(
            OutboxRepository repository,
            KafkaTemplate<String, String> kafka,
            OutboxProperties properties,
            Clock clock) {
        this.repository = repository;
        this.kafka = kafka;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int publishPending() {
        var published = 0;
        for (var event : repository.lockNextBatch(properties.batchSize())) {
            if (send(event)) {
                event.markPublished(clock.instant());
                published++;
            }
        }
        return published;
    }

    private boolean send(OutboxEvent event) {
        try {
            kafka.send(toRecord(event)).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException | KafkaException e) {
            log.warn("Could not publish outbox event {} ({}), will retry", event.getId(), e.toString());
            return false;
        }
    }

    private ProducerRecord<String, String> toRecord(OutboxEvent event) {
        var record = new ProducerRecord<>(properties.topic(), event.getAggregateId(), event.getPayload());
        record.headers().add(EVENT_ID_HEADER, bytes(event.getId().toString()));
        record.headers().add(EVENT_TYPE_HEADER, bytes(event.getEventType()));
        return record;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
