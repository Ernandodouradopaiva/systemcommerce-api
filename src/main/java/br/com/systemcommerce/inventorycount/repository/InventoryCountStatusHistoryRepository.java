package br.com.systemcommerce.inventorycount.repository;

import br.com.systemcommerce.inventorycount.entity.InventoryCountStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCountStatusHistoryRepository extends JpaRepository<InventoryCountStatusHistory, UUID> {

    List<InventoryCountStatusHistory> findByInventoryCountIdOrderByChangedAtAsc(UUID inventoryCountId);
}
