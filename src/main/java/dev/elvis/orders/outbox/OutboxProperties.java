package dev.elvis.orders.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("outbox")
public record OutboxProperties(String topic, int batchSize, Duration sendTimeout) {}
