package br.com.systemcommerce.inventorycount.repository;

import br.com.systemcommerce.inventorycount.entity.InventoryCountItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryCountItemRepository extends JpaRepository<InventoryCountItem, UUID> {

    @Query(
            """
            SELECT i FROM InventoryCountItem i
            JOIN FETCH i.product
            LEFT JOIN FETCH i.storageLocation
            WHERE i.inventoryCount.id = :countId AND i.active = true
            ORDER BY i.lineNumber ASC
            """)
    List<InventoryCountItem> findActiveByInventoryCountId(@Param("countId") UUID countId);

    int countByInventoryCountIdAndActiveTrue(UUID countId);
}
