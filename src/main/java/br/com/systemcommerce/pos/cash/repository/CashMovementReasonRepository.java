package br.com.systemcommerce.pos.cash.repository;

import br.com.systemcommerce.pos.cash.entity.CashMovementReason;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashMovementReasonRepository extends JpaRepository<CashMovementReason, UUID> {

    List<CashMovementReason> findByActiveTrueOrderByDescriptionAsc();

    @Query(
            """
            SELECT r FROM CashMovementReason r
            WHERE r.active = TRUE
              AND (r.appliesTo = :applies OR r.appliesTo = :both)
            ORDER BY r.description ASC
            """)
    List<CashMovementReason> findActiveFor(
            @Param("applies") CashMovementReason.AppliesTo applies,
            @Param("both") CashMovementReason.AppliesTo both);

    default List<CashMovementReason> findActiveFor(CashMovementReason.AppliesTo applies) {
        return findActiveFor(applies, CashMovementReason.AppliesTo.BOTH);
    }

    Optional<CashMovementReason> findByIdAndActiveTrue(UUID id);
}
