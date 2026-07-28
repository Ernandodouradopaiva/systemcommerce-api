package br.com.systemcommerce.finance.reconciliation.repository;

import br.com.systemcommerce.finance.reconciliation.entity.BankStatementImport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankStatementImportRepository extends JpaRepository<BankStatementImport, UUID> {
    Optional<BankStatementImport> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    boolean existsByHolderIdAndFileHash(UUID holderId, String fileHash);
}
