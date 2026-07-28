package br.com.systemcommerce.pos.cash.repository;

import br.com.systemcommerce.pos.cash.entity.CashMovement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {

    @EntityGraph(attributePaths = {"executedBy", "authorizedBy", "movementReason", "sale", "reversesMovement"})
    List<CashMovement> findByCashSessionIdOrderByOccurredAtAsc(UUID cashSessionId);

    @EntityGraph(attributePaths = {"executedBy", "authorizedBy", "movementReason", "sale", "reversesMovement"})
    Page<CashMovement> findByCashSessionId(UUID cashSessionId, Pageable pageable);

    @EntityGraph(attributePaths = {"executedBy", "authorizedBy", "movementReason", "sale", "cashSession", "reversesMovement"})
    @Query("SELECT m FROM CashMovement m WHERE m.id = :id")
    Optional<CashMovement> findDetailedById(@Param("id") UUID id);

    Optional<CashMovement> findByIdempotencyKey(String idempotencyKey);

    boolean existsByReversesMovementId(UUID reversesMovementId);

    @Query(
            """
            SELECT COALESCE(SUM(m.amount), 0) FROM CashMovement m
            WHERE m.cashSession.id = :sessionId AND m.type = :type
            """)
    BigDecimal sumAmountBySessionAndType(
            @Param("sessionId") UUID sessionId, @Param("type") CashMovement.MovementType type);

    @Query(
            """
            SELECT m.type, COALESCE(SUM(m.amount), 0)
            FROM CashMovement m
            WHERE m.cashSession.id = :sessionId
            GROUP BY m.type
            ORDER BY m.type
            """)
    List<Object[]> sumGroupedByType(@Param("sessionId") UUID sessionId);

    boolean existsByCashSessionIdAndTypeNotIn(UUID cashSessionId, java.util.Collection<CashMovement.MovementType> types);

    @Query(
            """
            SELECT COUNT(m) > 0 FROM CashMovement m
            WHERE m.cashSession.id = :sessionId
              AND m.type NOT IN ('OPENING')
            """)
    boolean existsNonOpeningBySessionId(@Param("sessionId") UUID sessionId);
}
