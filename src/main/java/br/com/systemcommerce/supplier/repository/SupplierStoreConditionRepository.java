package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierStoreCondition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierStoreConditionRepository extends JpaRepository<SupplierStoreCondition, UUID> {

    List<SupplierStoreCondition> findBySupplierIdOrderByCreatedAtAsc(UUID supplierId);

    Optional<SupplierStoreCondition> findBySupplierIdAndStoreId(UUID supplierId, UUID storeId);

    boolean existsBySupplierIdAndStoreId(UUID supplierId, UUID storeId);

    boolean existsBySupplierIdAndStoreIdAndIdNot(UUID supplierId, UUID storeId, UUID id);
}
