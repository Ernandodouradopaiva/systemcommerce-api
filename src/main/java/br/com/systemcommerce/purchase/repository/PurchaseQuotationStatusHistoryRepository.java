package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseQuotationStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseQuotationStatusHistoryRepository extends JpaRepository<PurchaseQuotationStatusHistory, UUID> {

    List<PurchaseQuotationStatusHistory> findByPurchaseQuotationIdOrderByChangedAtAsc(UUID purchaseQuotationId);
}
