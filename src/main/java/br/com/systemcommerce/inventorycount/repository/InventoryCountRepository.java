package br.com.systemcommerce.inventorycount.repository;

import br.com.systemcommerce.inventorycount.entity.InventoryCount;
import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryCountRepository
        extends JpaRepository<InventoryCount, UUID>, JpaSpecificationExecutor<InventoryCount> {

    @Query(
            """
            SELECT ic FROM InventoryCount ic
            LEFT JOIN FETCH ic.store
            LEFT JOIN FETCH ic.warehouse
            LEFT JOIN FETCH ic.organization
            WHERE ic.id = :id
            """)
    Optional<InventoryCount> findDetailedById(@Param("id") UUID id);

    Optional<InventoryCount> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    long countByOrganizationIdAndCountNumberStartingWith(UUID organizationId, String prefix);
}
