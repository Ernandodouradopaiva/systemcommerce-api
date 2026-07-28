package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseOrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderStatusHistoryRepository extends JpaRepository<PurchaseOrderStatusHistory, UUID> {

    List<PurchaseOrderStatusHistory> findByPurchaseOrderIdOrderByChangedAtAsc(UUID purchaseOrderId);
}
