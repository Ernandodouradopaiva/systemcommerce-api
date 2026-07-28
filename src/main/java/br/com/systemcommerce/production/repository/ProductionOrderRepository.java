package br.com.systemcommerce.production.repository;

import br.com.systemcommerce.production.entity.ProductionOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionOrderRepository
        extends JpaRepository<ProductionOrder, UUID>, JpaSpecificationExecutor<ProductionOrder> {

    @Query(
            """
            SELECT po FROM ProductionOrder po
            LEFT JOIN FETCH po.store
            LEFT JOIN FETCH po.warehouse
            LEFT JOIN FETCH po.billOfMaterials
            LEFT JOIN FETCH po.finishedProduct
            WHERE po.id = :id
            """)
    Optional<ProductionOrder> findDetailedById(@Param("id") UUID id);

    Optional<ProductionOrder> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    long countByOrderNumberStartingWith(String prefix);
}
