package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseRequestStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestStatusHistoryRepository extends JpaRepository<PurchaseRequestStatusHistory, UUID> {

    List<PurchaseRequestStatusHistory> findByPurchaseRequestIdOrderByChangedAtAsc(UUID purchaseRequestId);
}
