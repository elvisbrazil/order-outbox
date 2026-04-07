package dev.elvis.orders.history;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderHistoryRepository extends JpaRepository<OrderHistoryEntry, Long> {

    List<OrderHistoryEntry> findByOrderIdOrderByOccurredAt(UUID orderId);
}
