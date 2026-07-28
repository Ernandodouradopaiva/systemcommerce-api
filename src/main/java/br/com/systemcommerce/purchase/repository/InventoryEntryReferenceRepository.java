package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.InventoryEntryReference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryEntryReferenceRepository extends JpaRepository<InventoryEntryReference, UUID> {

    List<InventoryEntryReference> findByPurchaseReceiptId(UUID purchaseReceiptId);
}
