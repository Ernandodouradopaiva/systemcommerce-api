package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {

    boolean existsBySupplierId(UUID supplierId);

    @Query(
            """
            SELECT DISTINCT o FROM PurchaseOrder o
            LEFT JOIN FETCH o.items
            LEFT JOIN FETCH o.supplier
            LEFT JOIN FETCH o.buyer
            LEFT JOIN FETCH o.store
            LEFT JOIN FETCH o.destinationStore
            LEFT JOIN FETCH o.organization
            LEFT JOIN FETCH o.warehouse
            LEFT JOIN FETCH o.purchaseQuotation
            WHERE o.id = :id
            """)
    Optional<PurchaseOrder> findDetailedById(@Param("id") UUID id);
}
