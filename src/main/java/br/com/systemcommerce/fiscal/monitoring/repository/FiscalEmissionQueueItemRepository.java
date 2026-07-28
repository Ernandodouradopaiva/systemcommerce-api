package br.com.systemcommerce.fiscal.monitoring.repository;

import br.com.systemcommerce.fiscal.monitoring.entity.FiscalEmissionQueueItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalEmissionQueueItemRepository extends JpaRepository<FiscalEmissionQueueItem, UUID> {
    Optional<FiscalEmissionQueueItem> findByIdempotencyKey(String idempotencyKey);

    List<FiscalEmissionQueueItem> findByStatusOrderByPriorityAscCreatedAtAsc(FiscalEmissionQueueItem.Status status);
}
