package br.com.systemcommerce.pos.cancellation.repository;

import br.com.systemcommerce.pos.cancellation.entity.SaleReturnItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleReturnItemRepository extends JpaRepository<SaleReturnItem, UUID> {

    List<SaleReturnItem> findBySaleReturnIdOrderByCreatedAtAsc(UUID saleReturnId);
}
