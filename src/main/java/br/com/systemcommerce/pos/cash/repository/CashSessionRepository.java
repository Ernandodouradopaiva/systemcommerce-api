package br.com.systemcommerce.pos.cash.repository;

import br.com.systemcommerce.pos.cash.entity.CashSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashSessionRepository
        extends JpaRepository<CashSession, UUID>, JpaSpecificationExecutor<CashSession> {

    @EntityGraph(attributePaths = {"store", "terminal", "terminal.warehouse", "operator", "authorizedBy"})
    @Query("SELECT s FROM CashSession s WHERE s.id = :id")
    Optional<CashSession> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CashSession s WHERE s.id = :id")
    Optional<CashSession> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"store", "terminal", "operator"})
    @Query(
            """
            SELECT s FROM CashSession s
            WHERE s.operator.id = :operatorId AND s.status = 'OPEN'
            """)
    java.util.List<CashSession> findOpenByOperatorId(@Param("operatorId") UUID operatorId);

    @EntityGraph(attributePaths = {"store", "terminal", "operator"})
    @Query(
            """
            SELECT s FROM CashSession s
            WHERE s.terminal.id = :terminalId AND s.status IN ('OPEN', 'CLOSING')
            """)
    Optional<CashSession> findActiveByTerminalId(@Param("terminalId") UUID terminalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT s FROM CashSession s
            WHERE s.terminal.id = :terminalId AND s.status IN ('OPEN', 'CLOSING')
            """)
    Optional<CashSession> findActiveByTerminalIdForUpdate(@Param("terminalId") UUID terminalId);

    Optional<CashSession> findByOpenIdempotencyKey(String openIdempotencyKey);

    boolean existsByTerminalIdAndStatusIn(UUID terminalId, java.util.Collection<CashSession.CashSessionStatus> statuses);

    long countByStoreIdAndStatusIn(
            UUID storeId, java.util.Collection<CashSession.CashSessionStatus> statuses);
}
