package br.com.systemcommerce.finance.reconciliation.repository;

import br.com.systemcommerce.finance.reconciliation.entity.BankStatementEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankStatementEntryRepository extends JpaRepository<BankStatementEntry, UUID> {
    List<BankStatementEntry> findByStatementIdOrderByEntryDateAsc(UUID statementId);

    List<BankStatementEntry> findByHolderIdAndReconciliationStatus(
            UUID holderId, BankStatementEntry.ReconciliationStatus status);

    @Query("select e from BankStatementEntry e join fetch e.holder join fetch e.statement where e.id = :id")
    Optional<BankStatementEntry> findDetailedById(@Param("id") UUID id);
}
