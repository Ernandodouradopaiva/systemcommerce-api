package br.com.systemcommerce.uom.repository;

import br.com.systemcommerce.uom.entity.SupplierProductUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierProductUnitRepository extends JpaRepository<SupplierProductUnit, UUID> {

    List<SupplierProductUnit> findByProduct_IdAndSupplier_Id(UUID productId, UUID supplierId);

    List<SupplierProductUnit> findByProduct_Id(UUID productId);
}
