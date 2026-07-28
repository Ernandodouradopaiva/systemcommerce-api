package br.com.systemcommerce.batch.repository;

import br.com.systemcommerce.batch.entity.BatchInventory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchInventoryRepository extends JpaRepository<BatchInventory, UUID> {

    Optional<BatchInventory> findByProductBatchIdAndWarehouseIdAndActiveTrue(UUID productBatchId, UUID warehouseId);
}
