package br.com.systemcommerce.finance.reconciliation.repository;

import br.com.systemcommerce.finance.reconciliation.entity.BankStatement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID>, JpaSpecificationExecutor<BankStatement> {
    Optional<BankStatement> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    boolean existsByHolderIdAndExternalFileHash(UUID holderId, String hash);

    @Query("select s from BankStatement s join fetch s.holder left join fetch s.organization where s.id = :id")
    Optional<BankStatement> findDetailedById(@Param("id") UUID id);
}
