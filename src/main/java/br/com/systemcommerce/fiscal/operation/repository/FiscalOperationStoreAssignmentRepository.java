package br.com.systemcommerce.fiscal.operation.repository;

import br.com.systemcommerce.fiscal.operation.entity.FiscalOperationStoreAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalOperationStoreAssignmentRepository
        extends JpaRepository<FiscalOperationStoreAssignment, UUID> {

    List<FiscalOperationStoreAssignment> findByOperationId(UUID operationId);

    boolean existsByOperationIdAndStoreIdAndStatus(
            UUID operationId, UUID storeId, FiscalOperationStoreAssignment.AssignmentStatus status);
}
