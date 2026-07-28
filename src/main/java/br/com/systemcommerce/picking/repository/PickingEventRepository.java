package br.com.systemcommerce.picking.repository;

import br.com.systemcommerce.picking.entity.PickingEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickingEventRepository extends JpaRepository<PickingEvent, UUID> {

    List<PickingEvent> findByPickingOrderIdOrderByOccurredAtAsc(UUID pickingOrderId);

    Optional<PickingEvent> findByPickingOrderIdAndIdempotencyKey(UUID pickingOrderId, String idempotencyKey);
}
