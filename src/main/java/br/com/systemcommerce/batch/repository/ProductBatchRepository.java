package br.com.systemcommerce.batch.repository;

import br.com.systemcommerce.batch.entity.ProductBatch;
import br.com.systemcommerce.batch.entity.ProductBatchStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductBatchRepository
        extends JpaRepository<ProductBatch, UUID>, JpaSpecificationExecutor<ProductBatch> {

    @Query(
            """
            SELECT pb FROM ProductBatch pb
            LEFT JOIN FETCH pb.product
            LEFT JOIN FETCH pb.organization
            WHERE pb.id = :id
            """)
    Optional<ProductBatch> findDetailedById(@Param("id") UUID id);

    Optional<ProductBatch> findByOrganizationIdAndProductIdAndBatchCodeAndActiveTrue(
            UUID organizationId, UUID productId, String batchCode);

    @Query(
            """
            SELECT pb FROM ProductBatch pb
            JOIN BatchInventory bi ON bi.productBatch = pb AND bi.active = true
            WHERE pb.product.id = :productId
              AND bi.warehouse.id = :warehouseId
              AND pb.status = :status
              AND pb.active = true
            ORDER BY CASE WHEN pb.expiresAt IS NULL THEN 1 ELSE 0 END,
                     pb.expiresAt ASC NULLS LAST,
                     pb.receivedAt ASC NULLS LAST
            """)
    List<ProductBatch> findFefoCandidates(
            @Param("productId") UUID productId,
            @Param("warehouseId") UUID warehouseId,
            @Param("status") ProductBatchStatus status);
}
