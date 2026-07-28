package br.com.systemcommerce.fiscal.taxation.engine.repository;

import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculationItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxCalculationItemRepository extends JpaRepository<TaxCalculationItem, UUID> {

    List<TaxCalculationItem> findByCalculationIdOrderByLineNumber(UUID calculationId);
}
