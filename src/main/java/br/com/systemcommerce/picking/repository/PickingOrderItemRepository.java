package br.com.systemcommerce.picking.repository;

import br.com.systemcommerce.picking.entity.PickingOrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickingOrderItemRepository extends JpaRepository<PickingOrderItem, UUID> {

    List<PickingOrderItem> findByPickingOrderIdOrderByLineNumberAsc(UUID pickingOrderId);

    /**
     * IDs dos itens ordenados pelo código da localização de armazenagem (crescente, nulos por último) e,
     * em seguida, pelo número da linha. Consulta nativa porque não há entidade JPA para {@code storage_locations}
     * neste módulo (Prompt 71 usa apenas o ID de referência).
     */
    @Query(
            value =
                    """
                    SELECT poi.id
                    FROM picking_order_items poi
                    LEFT JOIN storage_locations sl ON sl.id = poi.storage_location_id
                    WHERE poi.picking_order_id = :pickingOrderId
                    ORDER BY sl.code ASC NULLS LAST, poi.line_number ASC
                    """,
            nativeQuery = true)
    List<UUID> findIdsOrderedByStorageLocationCode(@Param("pickingOrderId") UUID pickingOrderId);

    @Query(
            value =
                    """
                    SELECT sl.code
                    FROM storage_locations sl
                    WHERE sl.id = :storageLocationId
                    """,
            nativeQuery = true)
    String findStorageLocationCode(@Param("storageLocationId") UUID storageLocationId);

    /** Localização preferencial (ou a primeira disponível) do produto no depósito — melhor esforço. */
    @Query(
            value =
                    """
                    SELECT psl.storage_location_id
                    FROM product_storage_locations psl
                    JOIN storage_locations sl ON sl.id = psl.storage_location_id
                    WHERE psl.product_id = :productId AND sl.warehouse_id = :warehouseId AND psl.active = true
                    ORDER BY psl.preferred DESC, sl.code ASC
                    LIMIT 1
                    """,
            nativeQuery = true)
    UUID findPreferredStorageLocationId(
            @Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId);
}
