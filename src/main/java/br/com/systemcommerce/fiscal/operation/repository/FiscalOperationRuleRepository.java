package br.com.systemcommerce.fiscal.operation.repository;

import br.com.systemcommerce.fiscal.operation.entity.FiscalOperationRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalOperationRuleRepository extends JpaRepository<FiscalOperationRule, UUID> {

    List<FiscalOperationRule> findByOperationIdOrderByPriorityDesc(UUID operationId);
}
