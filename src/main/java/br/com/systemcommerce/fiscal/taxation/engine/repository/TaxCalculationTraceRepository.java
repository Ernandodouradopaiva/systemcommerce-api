package br.com.systemcommerce.fiscal.taxation.engine.repository;

import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculationTrace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxCalculationTraceRepository extends JpaRepository<TaxCalculationTrace, UUID> {

    List<TaxCalculationTrace> findByCalculationIdOrderByStepOrder(UUID calculationId);
}
