package br.com.systemcommerce.picking.repository;

import br.com.systemcommerce.picking.entity.PickingOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickingOrderRepository
        extends JpaRepository<PickingOrder, UUID>, JpaSpecificationExecutor<PickingOrder> {

    @Query(
            """
            SELECT DISTINCT p FROM PickingOrder p
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.product
            LEFT JOIN FETCH p.store
            LEFT JOIN FETCH p.warehouse
            LEFT JOIN FETCH p.organization
            LEFT JOIN FETCH p.salesOrder
            LEFT JOIN FETCH p.assignedTo
            WHERE p.id = :id
            """)
    Optional<PickingOrder> findDetailedById(@Param("id") UUID id);

    List<PickingOrder> findBySalesOrderId(UUID salesOrderId);

    @Query("SELECT COUNT(p) FROM PickingOrder p WHERE p.pickingNumber LIKE CONCAT(:prefix, '%')")
    long countByPickingNumberPrefix(@Param("prefix") String prefix);
}
