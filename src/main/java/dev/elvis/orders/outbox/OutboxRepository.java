package dev.elvis.orders.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(
            value =
                    """
                    select * from outbox_event
                    where published_at is null
                    order by created_at
                    limit :limit
                    for update skip locked
                    """,
            nativeQuery = true)
    List<OutboxEvent> lockNextBatch(int limit);

    List<OutboxEvent> findByAggregateIdOrderByCreatedAt(String aggregateId);
}
