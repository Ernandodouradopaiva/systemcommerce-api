package br.com.systemcommerce.fiscal.operation.repository;

import br.com.systemcommerce.fiscal.operation.entity.FiscalOperationItemRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalOperationItemRuleRepository extends JpaRepository<FiscalOperationItemRule, UUID> {

    List<FiscalOperationItemRule> findByOperationId(UUID operationId);
}
