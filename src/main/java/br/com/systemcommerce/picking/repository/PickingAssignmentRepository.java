package br.com.systemcommerce.picking.repository;

import br.com.systemcommerce.picking.entity.PickingAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickingAssignmentRepository extends JpaRepository<PickingAssignment, UUID> {

    List<PickingAssignment> findByPickingOrderIdOrderByAssignedAtDesc(UUID pickingOrderId);
}
