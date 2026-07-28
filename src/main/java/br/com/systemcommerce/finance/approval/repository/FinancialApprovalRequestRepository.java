package br.com.systemcommerce.finance.approval.repository;

import br.com.systemcommerce.finance.approval.entity.FinancialApprovalRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialApprovalRequestRepository extends JpaRepository<FinancialApprovalRequest, UUID> {
    Optional<FinancialApprovalRequest> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    List<FinancialApprovalRequest> findByOrganizationIdAndStatusOrderByRequestedAtDesc(
            UUID organizationId, FinancialApprovalRequest.Status status);

    Optional<FinancialApprovalRequest> findByIdAndStatus(UUID id, FinancialApprovalRequest.Status status);
}
