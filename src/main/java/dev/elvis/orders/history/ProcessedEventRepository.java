package dev.elvis.orders.history;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
