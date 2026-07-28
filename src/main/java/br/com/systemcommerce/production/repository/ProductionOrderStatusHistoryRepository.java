package br.com.systemcommerce.production.repository;

import br.com.systemcommerce.production.entity.ProductionOrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionOrderStatusHistoryRepository extends JpaRepository<ProductionOrderStatusHistory, UUID> {

    List<ProductionOrderStatusHistory> findByProductionOrderIdOrderByChangedAtAsc(UUID productionOrderId);
}
