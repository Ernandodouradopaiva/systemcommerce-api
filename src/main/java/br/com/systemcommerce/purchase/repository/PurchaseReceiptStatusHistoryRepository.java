package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseReceiptStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptStatusHistoryRepository extends JpaRepository<PurchaseReceiptStatusHistory, UUID> {

    List<PurchaseReceiptStatusHistory> findByPurchaseReceiptIdOrderByChangedAtAsc(UUID purchaseReceiptId);
}
