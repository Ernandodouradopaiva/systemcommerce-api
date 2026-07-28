package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierCommercialCondition;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierCommercialConditionRepository extends JpaRepository<SupplierCommercialCondition, UUID> {

    Optional<SupplierCommercialCondition> findBySupplierId(UUID supplierId);
}
