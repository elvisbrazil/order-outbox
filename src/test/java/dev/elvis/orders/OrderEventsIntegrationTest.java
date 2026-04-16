package dev.elvis.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.elvis.orders.history.OrderHistoryEntry;
import dev.elvis.orders.history.OrderHistoryProjector;
import dev.elvis.orders.order.OrderService;
import dev.elvis.orders.outbox.OutboxEvent;
import dev.elvis.orders.outbox.OutboxRelay;
import dev.elvis.orders.outbox.OutboxRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "orders.events")
class OrderEventsIntegrationTest {

    private static final String TOPIC = "orders.events";

    @Autowired
    private OrderService orders;

    @Autowired
    private OutboxRepository outbox;

    @Autowired
    private OrderHistoryProjector history;

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    @BeforeEach
    void subscribe() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("it-" + UUID.randomUUID(), "false", broker);
        consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
    }

    @AfterEach
    void close() {
        consumer.close();
    }

    @Test
    void createdOrderIsPublishedToKafkaKeyedByOrderId() throws Exception {
        var order = orders.create("customer-42", new BigDecimal("120.50"));
        var key = order.getId().toString();

        var record = awaitRecordWithKey(key);

        assertThat(header(record, OutboxRelay.EVENT_TYPE_HEADER)).isEqualTo("OrderCreated");
        var payload = objectMapper.readTree(record.value());
        assertThat(payload.get("orderId").asText()).isEqualTo(key);
        assertThat(payload.get("customerId").asText()).isEqualTo("customer-42");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("120.50");
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(outbox.findByAggregateIdOrderByCreatedAt(key))
                        .singleElement()
                        .satisfies(event -> assertThat(event.isPublished()).isTrue()));
    }

    @Test
    void historyProjectionFollowsOrderLifecycle() {
        var order = orders.create("customer-7", new BigDecimal("15.00"));
        orders.pay(order.getId());
        orders.cancel(order.getId());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(history.historyOf(order.getId()))
                .extracting(OrderHistoryEntry::getEventType)
                .containsExactly("OrderCreated", "OrderPaid", "OrderCancelled"));
    }

    @Test
    void redeliveredEventIsProjectedOnce() throws Exception {
        var order = orders.create("customer-9", new BigDecimal("8.00"));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(history.historyOf(order.getId()))
                .hasSize(1));
        var created = outbox.findByAggregateIdOrderByCreatedAt(order.getId().toString())
                .getFirst();

        kafka.send(copyOf(created)).get();
        orders.pay(order.getId());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(history.historyOf(order.getId()))
                .extracting(OrderHistoryEntry::getEventType)
                .containsExactly("OrderCreated", "OrderPaid"));
    }

    private ConsumerRecord<String, String> awaitRecordWithKey(String key) {
        var received = new ArrayList<ConsumerRecord<String, String>>();
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            KafkaTestUtils.getRecords(consumer, Duration.ofMillis(200)).forEach(received::add);
            return received.stream().anyMatch(r -> key.equals(r.key()));
        });
        return received.stream().filter(r -> key.equals(r.key())).findFirst().orElseThrow();
    }

    private static ProducerRecord<String, String> copyOf(OutboxEvent event) {
        var record = new ProducerRecord<>(TOPIC, event.getAggregateId(), event.getPayload());
        record.headers().add(OutboxRelay.EVENT_ID_HEADER, bytes(event.getId().toString()));
        record.headers().add(OutboxRelay.EVENT_TYPE_HEADER, bytes(event.getEventType()));
        return record;
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
