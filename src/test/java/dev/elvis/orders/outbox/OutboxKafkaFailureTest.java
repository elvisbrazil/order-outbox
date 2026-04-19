package dev.elvis.orders.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import dev.elvis.orders.order.OrderService;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:outbox-failure;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "outbox.relay.enabled=false",
            "spring.kafka.admin.auto-create=false",
            "spring.kafka.listener.auto-startup=false"
        })
@ActiveProfiles("test")
class OutboxKafkaFailureTest {

    @Autowired
    private OrderService orders;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private OutboxRepository outbox;

    @MockitoBean
    private KafkaTemplate<String, String> kafka;

    @Test
    void keepsEventUnpublishedWhenKafkaIsDownAndPublishesItOnRetry() {
        var order = orders.create("customer-1", new BigDecimal("99.99"));
        var aggregateId = order.getId().toString();
        doReturn(CompletableFuture.failedFuture(new TimeoutException("Topic orders.events not present in metadata")))
                .when(kafka)
                .send(any(ProducerRecord.class));

        assertThat(relay.publishPending()).isZero();
        assertThat(outbox.findByAggregateIdOrderByCreatedAt(aggregateId))
                .singleElement()
                .satisfies(event -> assertThat(event.getPublishedAt()).isNull());

        doReturn(CompletableFuture.completedFuture(null)).when(kafka).send(any(ProducerRecord.class));

        assertThat(relay.publishPending()).isEqualTo(1);
        assertThat(outbox.findByAggregateIdOrderByCreatedAt(aggregateId))
                .singleElement()
                .satisfies(event -> assertThat(event.getPublishedAt()).isNotNull());
    }
}
