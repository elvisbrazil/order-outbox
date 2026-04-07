package dev.elvis.orders.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.elvis.orders.outbox.OutboxRelay;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderHistoryListener {

    private final OrderHistoryProjector projector;
    private final ObjectMapper objectMapper;

    OrderHistoryListener(OrderHistoryProjector projector, ObjectMapper objectMapper) {
        this.projector = projector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${outbox.topic}", groupId = "order-history")
    void on(ConsumerRecord<String, String> record) throws JsonProcessingException {
        var payload = objectMapper.readTree(record.value());
        projector.apply(new ReceivedEvent(
                UUID.fromString(header(record, OutboxRelay.EVENT_ID_HEADER)),
                UUID.fromString(record.key()),
                header(record, OutboxRelay.EVENT_TYPE_HEADER),
                Instant.parse(payload.get("occurredAt").asText())));
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        var header = record.headers().lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("Missing header " + name);
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
