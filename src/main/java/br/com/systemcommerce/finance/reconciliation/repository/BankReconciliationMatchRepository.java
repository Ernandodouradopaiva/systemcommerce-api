package br.com.systemcommerce.finance.reconciliation.repository;

import br.com.systemcommerce.finance.reconciliation.entity.BankReconciliationMatch;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankReconciliationMatchRepository extends JpaRepository<BankReconciliationMatch, UUID> {
    boolean existsByHolderMovementIdAndMatchStatus(UUID holderMovementId, BankReconciliationMatch.MatchStatus status);

    @Query("select m from BankReconciliationMatch m join fetch m.statementEntry join fetch m.reconciliation where m.id = :id")
    Optional<BankReconciliationMatch> findDetailedById(@Param("id") UUID id);
}
