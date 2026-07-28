package br.com.systemcommerce.finance.advance.repository;

import br.com.systemcommerce.finance.advance.entity.AdvanceApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvanceApplicationRepository extends JpaRepository<AdvanceApplication, UUID> {
    Optional<AdvanceApplication> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    List<AdvanceApplication> findByCustomerAdvanceIdOrderByCreatedAtAsc(UUID customerAdvanceId);

    List<AdvanceApplication> findBySupplierAdvanceIdOrderByCreatedAtAsc(UUID supplierAdvanceId);
}
