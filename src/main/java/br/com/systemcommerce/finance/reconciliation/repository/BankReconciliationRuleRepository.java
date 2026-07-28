package br.com.systemcommerce.finance.reconciliation.repository;

import br.com.systemcommerce.finance.reconciliation.entity.BankReconciliationRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankReconciliationRuleRepository extends JpaRepository<BankReconciliationRule, UUID> {
    List<BankReconciliationRule> findByOrganizationIdAndStatusOrderByPriorityAsc(
            UUID organizationId, BankReconciliationRule.Status status);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
