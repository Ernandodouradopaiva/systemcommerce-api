package br.com.systemcommerce.production.repository;

import br.com.systemcommerce.production.entity.ProductionConsumption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionConsumptionRepository extends JpaRepository<ProductionConsumption, UUID> {

    List<ProductionConsumption> findByProductionOrderId(UUID productionOrderId);
}
