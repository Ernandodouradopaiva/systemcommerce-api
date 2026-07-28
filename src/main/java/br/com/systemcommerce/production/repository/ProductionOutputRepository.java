package br.com.systemcommerce.production.repository;

import br.com.systemcommerce.production.entity.ProductionOutput;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionOutputRepository extends JpaRepository<ProductionOutput, UUID> {

    List<ProductionOutput> findByProductionOrderId(UUID productionOrderId);
}
