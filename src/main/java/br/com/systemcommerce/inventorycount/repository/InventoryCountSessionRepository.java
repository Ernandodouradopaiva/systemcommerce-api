package br.com.systemcommerce.inventorycount.repository;

import br.com.systemcommerce.inventorycount.entity.InventoryCountSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCountSessionRepository extends JpaRepository<InventoryCountSession, UUID> {

    int countByInventoryCountId(UUID inventoryCountId);
}
