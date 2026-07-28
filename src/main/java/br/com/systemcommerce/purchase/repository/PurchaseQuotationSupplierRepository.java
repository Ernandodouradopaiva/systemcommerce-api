package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseQuotationSupplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseQuotationSupplierRepository extends JpaRepository<PurchaseQuotationSupplier, UUID> {

    List<PurchaseQuotationSupplier> findByPurchaseQuotationId(UUID purchaseQuotationId);

    Optional<PurchaseQuotationSupplier> findByPurchaseQuotationIdAndSupplierId(
            UUID purchaseQuotationId, UUID supplierId);
}
