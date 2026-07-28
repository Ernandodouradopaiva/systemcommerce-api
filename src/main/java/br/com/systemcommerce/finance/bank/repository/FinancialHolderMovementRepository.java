package br.com.systemcommerce.finance.bank.repository;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialHolderMovementRepository extends JpaRepository<FinancialHolderMovement, UUID> {
    long countByHolderId(UUID holderId);

    @Query(
            """
            select coalesce(sum(m.amount), 0) from FinancialHolderMovement m
            where m.holder.id = :holderId and m.reversed = false and m.active = true
            """)
    BigDecimal sumActiveAmount(@Param("holderId") UUID holderId);

    List<FinancialHolderMovement> findByHolderIdOrderByOccurredAtDesc(UUID holderId);

    @Query(
            """
            select m from FinancialHolderMovement m
            where m.holder.id = :holderId
              and m.reversed = false
              and m.active = true
              and abs(m.amount) = :absAmount
              and m.occurredAt >= :fromInstant
              and m.occurredAt < :toInstant
            order by m.occurredAt asc
            """)
    List<FinancialHolderMovement> findCandidatesForReconciliation(
            @Param("holderId") UUID holderId,
            @Param("absAmount") BigDecimal absAmount,
            @Param("fromInstant") java.time.Instant fromInstant,
            @Param("toInstant") java.time.Instant toInstant);
}
