package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.SupplierReturn;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierReturnRepository
        extends JpaRepository<SupplierReturn, UUID>, JpaSpecificationExecutor<SupplierReturn> {

    @Query(
            """
            SELECT DISTINCT r FROM SupplierReturn r
            LEFT JOIN FETCH r.items
            LEFT JOIN FETCH r.supplier
            LEFT JOIN FETCH r.store
            LEFT JOIN FETCH r.organization
            LEFT JOIN FETCH r.warehouse
            LEFT JOIN FETCH r.purchaseOrder
            LEFT JOIN FETCH r.purchaseReceipt
            WHERE r.id = :id
            """)
    Optional<SupplierReturn> findDetailedById(@Param("id") UUID id);
}
