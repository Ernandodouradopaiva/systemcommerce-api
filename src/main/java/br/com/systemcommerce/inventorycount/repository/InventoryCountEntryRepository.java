package br.com.systemcommerce.inventorycount.repository;

import br.com.systemcommerce.inventorycount.entity.InventoryCountEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCountEntryRepository extends JpaRepository<InventoryCountEntry, UUID> {

    Optional<InventoryCountEntry> findByInventoryCountIdAndIdempotencyKey(UUID inventoryCountId, String idempotencyKey);
}
