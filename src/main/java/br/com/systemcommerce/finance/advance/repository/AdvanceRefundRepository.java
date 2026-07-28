package br.com.systemcommerce.finance.advance.repository;

import br.com.systemcommerce.finance.advance.entity.AdvanceRefund;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvanceRefundRepository extends JpaRepository<AdvanceRefund, UUID> {
    Optional<AdvanceRefund> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);
}
