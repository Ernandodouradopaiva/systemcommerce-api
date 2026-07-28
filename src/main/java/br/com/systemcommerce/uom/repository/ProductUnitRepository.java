package br.com.systemcommerce.uom.repository;

import br.com.systemcommerce.uom.entity.ProductUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, UUID> {

    Optional<ProductUnit> findByProduct_Id(UUID productId);

    boolean existsByProduct_Id(UUID productId);
}
