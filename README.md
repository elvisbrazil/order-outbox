# order-outbox

A small Spring Boot order service that publishes domain events to Kafka using the transactional outbox pattern. Every state change of an order (created, paid, cancelled) writes a row to an `outbox_event` table in the same database transaction as the order itself. A scheduled relay picks up unpublished rows, sends them to Kafka keyed by order id and marks them as published. An idempotent consumer in the same app builds a per-order history from those events.

## Running

Requirements: Java 21 and Docker (only for running the app locally; the tests use H2 and an embedded Kafka broker).

```
./mvnw verify                 # tests, Spotless and Checkstyle
docker compose up -d          # PostgreSQL 16 + single-node Kafka (KRaft)
./mvnw spring-boot:run
```

## Usage

```
curl -i -X POST localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId": "c-123", "amount": 59.90}'

curl -X POST localhost:8080/orders/{id}/pay
curl -X POST localhost:8080/orders/{id}/cancel
curl localhost:8080/orders/{id}
curl localhost:8080/orders/{id}/history
```

To watch the raw events:

```
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic orders.events --from-beginning \
  --property print.key=true --property print.headers=true
```

Invalid transitions (paying a cancelled order, cancelling twice) return `409`, unknown ids return `404`, both as `application/problem+json`.

## Design notes

- The order and its event are committed together, so there is no window where the order exists but the event is lost (or the other way around). Publishing happens later and asynchronously.
- The relay claims a batch with `select ... for update skip locked`, so several instances can run the relay without sending the same rows at the same time.
- Each row is sent and acknowledged before it is marked as published. On the first failure the relay stops the batch and leaves the remaining rows for the next run, which keeps events of the same order in sequence. The downside is that a single row that can never be sent blocks the ones behind it; a real system would want an attempt counter and a parking table.
- Delivery is at-least-once: if the database commit fails after Kafka acknowledged a send, the same event goes out again. The consumer stores processed event ids (`processed_event`) in the same transaction as the projection and skips ids it has already seen.
- The event id and type travel as Kafka headers, the payload is plain JSON. Using the order id as key keeps all events of one order on the same partition.
- The payload column is `text` rather than `jsonb` so the same Flyway scripts run on H2 in tests.
