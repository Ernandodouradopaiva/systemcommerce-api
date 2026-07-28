package br.com.systemcommerce.stocktransfer.repository;

import br.com.systemcommerce.stocktransfer.entity.StockTransferItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, UUID> {

    @Query(
            """
            SELECT i FROM StockTransferItem i
            JOIN FETCH i.product
            WHERE i.transfer.id = :transferId AND i.active = true
            ORDER BY i.createdAt
            """)
    List<StockTransferItem> findActiveByTransferId(@Param("transferId") UUID transferId);

    Optional<StockTransferItem> findByTransferIdAndProductIdAndActiveTrue(UUID transferId, UUID productId);

    boolean existsByTransferIdAndActiveTrue(UUID transferId);

    @Query(
            """
            SELECT i FROM StockTransferItem i
            JOIN FETCH i.product p
            JOIN FETCH i.transfer t
            JOIN FETCH t.originStore
            JOIN FETCH t.originWarehouse
            JOIN FETCH t.destinationStore
            JOIN FETCH t.destinationWarehouse
            WHERE t.status IN ('IN_TRANSIT', 'PARTIALLY_RECEIVED', 'DISPATCHED')
              AND i.active = true
              AND (:storeId IS NULL OR t.originStore.id = :storeId OR t.destinationStore.id = :storeId)
              AND (i.quantityDispatched - i.quantityReceived - i.quantityDivergent) > 0
            ORDER BY t.dispatchedAt DESC, p.sku
            """)
    List<StockTransferItem> findInTransitItems(@Param("storeId") UUID storeId);
}
