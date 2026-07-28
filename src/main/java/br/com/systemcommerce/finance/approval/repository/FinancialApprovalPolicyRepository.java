package br.com.systemcommerce.finance.approval.repository;

import br.com.systemcommerce.finance.approval.entity.FinancialApprovalPolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialApprovalPolicyRepository extends JpaRepository<FinancialApprovalPolicy, UUID> {
    Optional<FinancialApprovalPolicy> findByOrganizationId(UUID organizationId);
}
