package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReceiptRepository
        extends JpaRepository<PurchaseReceipt, UUID>, JpaSpecificationExecutor<PurchaseReceipt> {

    @Query(
            """
            SELECT DISTINCT r FROM PurchaseReceipt r
            LEFT JOIN FETCH r.items i
            LEFT JOIN FETCH i.purchaseOrderItem
            LEFT JOIN FETCH r.purchaseOrder
            LEFT JOIN FETCH r.supplier
            LEFT JOIN FETCH r.store
            LEFT JOIN FETCH r.organization
            LEFT JOIN FETCH r.warehouse
            LEFT JOIN FETCH r.receivedBy
            WHERE r.id = :id
            """)
    Optional<PurchaseReceipt> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            SELECT COUNT(r) FROM PurchaseReceipt r
            WHERE r.organization.id = :organizationId
              AND r.receiptNumber LIKE CONCAT(:prefix, '%')
            """)
    long countByNumberPrefix(@Param("organizationId") UUID organizationId, @Param("prefix") String prefix);
}
