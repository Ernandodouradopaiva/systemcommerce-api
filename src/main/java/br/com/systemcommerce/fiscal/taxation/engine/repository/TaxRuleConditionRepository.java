package br.com.systemcommerce.fiscal.taxation.engine.repository;

import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRuleCondition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRuleConditionRepository extends JpaRepository<TaxRuleCondition, UUID> {

    List<TaxRuleCondition> findByRuleIdOrderBySortOrder(UUID ruleId);
}
