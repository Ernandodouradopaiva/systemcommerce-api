package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierProduct;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, UUID> {

    List<SupplierProduct> findBySupplierIdOrderByCreatedAtAsc(UUID supplierId);

    boolean existsBySupplierIdAndProductId(UUID supplierId, UUID productId);

    boolean existsBySupplierIdAndProductIdAndIdNot(UUID supplierId, UUID productId, UUID id);
}
