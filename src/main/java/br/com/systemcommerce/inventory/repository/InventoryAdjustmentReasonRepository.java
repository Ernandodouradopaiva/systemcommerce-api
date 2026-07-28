package br.com.systemcommerce.inventory.repository;

import br.com.systemcommerce.inventory.entity.InventoryAdjustmentReason;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentReasonRepository extends JpaRepository<InventoryAdjustmentReason, UUID> {

    Optional<InventoryAdjustmentReason> findByIdAndActiveTrue(UUID id);

    List<InventoryAdjustmentReason> findByActiveTrueOrderByDescriptionAsc();

    Optional<InventoryAdjustmentReason> findByCodeAndActiveTrue(String code);
}
