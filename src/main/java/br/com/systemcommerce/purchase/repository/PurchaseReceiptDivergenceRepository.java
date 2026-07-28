package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseReceiptDivergence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptDivergenceRepository extends JpaRepository<PurchaseReceiptDivergence, UUID> {

    List<PurchaseReceiptDivergence> findByPurchaseReceiptId(UUID purchaseReceiptId);
}
