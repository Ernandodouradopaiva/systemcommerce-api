package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseQuotationRepository
        extends JpaRepository<PurchaseQuotation, UUID>, JpaSpecificationExecutor<PurchaseQuotation> {

    @Query(
            """
            SELECT DISTINCT q FROM PurchaseQuotation q
            LEFT JOIN FETCH q.items
            LEFT JOIN FETCH q.suppliers s
            LEFT JOIN FETCH s.supplier
            LEFT JOIN FETCH q.buyer
            LEFT JOIN FETCH q.store
            LEFT JOIN FETCH q.organization
            LEFT JOIN FETCH q.purchaseRequest
            WHERE q.id = :id
            """)
    Optional<PurchaseQuotation> findDetailedById(@Param("id") UUID id);
}
