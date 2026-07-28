package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.PrivilegedAccessApproval;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegedAccessApprovalRepository extends JpaRepository<PrivilegedAccessApproval, UUID> {
    List<PrivilegedAccessApproval> findByRequestId(UUID requestId);

    long countByRequestIdAndDecision(UUID requestId, PrivilegedAccessApproval.Decision decision);
}
