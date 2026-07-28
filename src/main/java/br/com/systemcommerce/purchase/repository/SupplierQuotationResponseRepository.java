package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.SupplierQuotationResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierQuotationResponseRepository extends JpaRepository<SupplierQuotationResponse, UUID> {

    Optional<SupplierQuotationResponse> findByPurchaseQuotationIdAndSupplierId(
            UUID purchaseQuotationId, UUID supplierId);

    @Query(
            """
            SELECT DISTINCT r FROM SupplierQuotationResponse r
            LEFT JOIN FETCH r.items
            WHERE r.purchaseQuotation.id = :quotationId
            """)
    List<SupplierQuotationResponse> findDetailedByPurchaseQuotationId(@Param("quotationId") UUID quotationId);
}
