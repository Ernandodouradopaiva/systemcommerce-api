package br.com.systemcommerce.finance.reconciliation.repository;

import br.com.systemcommerce.finance.reconciliation.entity.BankReconciliation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, UUID> {
    Optional<BankReconciliation> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("select r from BankReconciliation r join fetch r.holder where r.id = :id")
    Optional<BankReconciliation> findDetailedById(@Param("id") UUID id);
}
