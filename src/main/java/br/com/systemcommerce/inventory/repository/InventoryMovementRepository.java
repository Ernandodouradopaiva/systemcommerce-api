package br.com.systemcommerce.inventory.repository;

import br.com.systemcommerce.inventory.entity.InventoryMovement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, UUID>, JpaSpecificationExecutor<InventoryMovement> {

    @Query("SELECT COUNT(m) > 0 FROM InventoryMovement m WHERE m.product.id = :productId")
    boolean existsByProductId(@Param("productId") UUID productId);
}
