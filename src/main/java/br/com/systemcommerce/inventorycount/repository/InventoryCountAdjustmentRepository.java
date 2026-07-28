package br.com.systemcommerce.inventorycount.repository;

import br.com.systemcommerce.inventorycount.entity.InventoryCountAdjustment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCountAdjustmentRepository extends JpaRepository<InventoryCountAdjustment, UUID> {}
