package br.com.systemcommerce.fiscal.taxation.engine.repository;

import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRuleResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRuleResultRepository extends JpaRepository<TaxRuleResult, UUID> {

    List<TaxRuleResult> findByRuleId(UUID ruleId);
}
