package br.com.systemcommerce.stockentry.repository;

import br.com.systemcommerce.stockentry.entity.StockEntryItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryItemRepository extends JpaRepository<StockEntryItem, UUID> {

    Optional<StockEntryItem> findByEntryIdAndProductIdAndActiveTrue(UUID entryId, UUID productId);

    boolean existsByEntryIdAndActiveTrue(UUID entryId);
}
