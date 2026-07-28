package br.com.systemcommerce.finance.costcenter.repository;

import br.com.systemcommerce.finance.costcenter.entity.CostCenterStoreAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterStoreAssignmentRepository extends JpaRepository<CostCenterStoreAssignment, UUID> {
    List<CostCenterStoreAssignment> findByCostCenterId(UUID costCenterId);

    boolean existsByCostCenterIdAndStoreId(UUID costCenterId, UUID storeId);
}
