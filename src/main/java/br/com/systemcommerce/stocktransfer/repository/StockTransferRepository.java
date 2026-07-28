package br.com.systemcommerce.stocktransfer.repository;

import br.com.systemcommerce.stocktransfer.entity.StockTransfer;
import br.com.systemcommerce.stocktransfer.entity.StockTransferStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, UUID>, JpaSpecificationExecutor<StockTransfer> {

    @Query(
            """
            SELECT t FROM StockTransfer t
            LEFT JOIN FETCH t.originStore
            LEFT JOIN FETCH t.originWarehouse
            LEFT JOIN FETCH t.destinationStore
            LEFT JOIN FETCH t.destinationWarehouse
            LEFT JOIN FETCH t.organization
            WHERE t.id = :id
            """)
    Optional<StockTransfer> findDetailedById(@Param("id") UUID id);

    Optional<StockTransfer> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query(
            """
            SELECT COUNT(t) FROM StockTransfer t
            WHERE t.organization.id = :organizationId
              AND t.number LIKE CONCAT(:prefix, '%')
            """)
    long countByNumberPrefix(@Param("organizationId") UUID organizationId, @Param("prefix") String prefix);

    @Query(
            """
            SELECT COUNT(t) > 0 FROM StockTransfer t
            WHERE t.active = true
              AND (t.originStore.id = :storeId OR t.destinationStore.id = :storeId)
              AND t.status NOT IN :terminalStatuses
            """)
    boolean existsPendingByStoreId(
            @Param("storeId") UUID storeId, @Param("terminalStatuses") Collection<StockTransferStatus> terminalStatuses);
}
