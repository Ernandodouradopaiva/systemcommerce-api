package br.com.systemcommerce.uom.repository;

import br.com.systemcommerce.uom.entity.SalesProductUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesProductUnitRepository extends JpaRepository<SalesProductUnit, UUID> {

    List<SalesProductUnit> findByProduct_Id(UUID productId);
}
