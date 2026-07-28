package br.com.systemcommerce.picking.repository;

import br.com.systemcommerce.picking.entity.PickingDivergence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickingDivergenceRepository extends JpaRepository<PickingDivergence, UUID> {

    List<PickingDivergence> findByPickingOrderIdOrderByCreatedAtAsc(UUID pickingOrderId);
}
