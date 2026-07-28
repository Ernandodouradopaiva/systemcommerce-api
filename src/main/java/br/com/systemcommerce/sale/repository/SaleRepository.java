package br.com.systemcommerce.sale.repository;

import br.com.systemcommerce.sale.entity.Sale;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    @Query("SELECT COUNT(s) > 0 FROM Sale s WHERE s.customer.id = :customerId")
    boolean hasSalesForCustomer(@Param("customerId") UUID customerId);

    @Query(
            """
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.customer
            LEFT JOIN FETCH s.seller
            LEFT JOIN FETCH s.sellerProfile sp
            LEFT JOIN FETCH sp.employee
            LEFT JOIN FETCH s.organization
            LEFT JOIN FETCH s.priceTable
            LEFT JOIN FETCH s.supervisor
            LEFT JOIN FETCH s.store
            LEFT JOIN FETCH s.terminal
            LEFT JOIN FETCH s.cashSession
            LEFT JOIN FETCH s.warehouse
            LEFT JOIN FETCH s.suspendedBy
            LEFT JOIN FETCH s.suspendedTerminal
            LEFT JOIN FETCH s.editLockOwner
            LEFT JOIN FETCH s.editLockTerminal
            WHERE s.id = :id
            """)
    Optional<Sale> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") UUID id);

    Optional<Sale> findByIdempotencyKey(String idempotencyKey);

    Optional<Sale> findByLastOperationIdempotencyKey(String lastOperationIdempotencyKey);

    @Query(
            """
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.customer
            LEFT JOIN FETCH s.seller
            LEFT JOIN FETCH s.store
            LEFT JOIN FETCH s.terminal
            LEFT JOIN FETCH s.cashSession
            LEFT JOIN FETCH s.warehouse
            WHERE s.idempotencyKey = :key OR s.lastOperationIdempotencyKey = :key
            """)
    Optional<Sale> findByAnyIdempotencyKey(@Param("key") String key);

    @Query(
            """
            SELECT s FROM Sale s
            WHERE s.channel = :channel
              AND s.terminal.id = :terminalId
              AND s.seller.id = :operatorId
              AND s.cashSession.id = :sessionId
              AND s.status = :status
            ORDER BY s.saleDate DESC
            """)
    List<Sale> findCurrentPosDrafts(
            @Param("terminalId") UUID terminalId,
            @Param("operatorId") UUID operatorId,
            @Param("sessionId") UUID sessionId,
            @Param("channel") Sale.SaleChannel channel,
            @Param("status") Sale.SaleStatus status);

    default List<Sale> findCurrentPosDrafts(UUID terminalId, UUID operatorId, UUID sessionId) {
        return findCurrentPosDrafts(
                terminalId, operatorId, sessionId, Sale.SaleChannel.POS, Sale.SaleStatus.DRAFT);
    }

    @Query(
            """
            SELECT s FROM Sale s
            WHERE s.channel = :channel
              AND s.status = :status
              AND s.id = :id
            """)
    Optional<Sale> findSuspendedPosById(
            @Param("id") UUID id,
            @Param("channel") Sale.SaleChannel channel,
            @Param("status") Sale.SaleStatus status);

    @Query(value = "SELECT nextval('sale_number_seq')", nativeQuery = true)
    Long nextSaleNumberValue();

    @Query(
            """
            SELECT COUNT(s) FROM Sale s
            WHERE s.cashSession.id = :sessionId
              AND s.status IN :statuses
            """)
    long countByCashSessionIdAndStatusIn(
            @Param("sessionId") UUID sessionId, @Param("statuses") List<Sale.SaleStatus> statuses);

    @Query(
            """
            SELECT COUNT(s) FROM Sale s
            WHERE s.cashSession.id = :sessionId
              AND s.status = :status
            """)
    long countByCashSessionIdAndStatus(
            @Param("sessionId") UUID sessionId, @Param("status") Sale.SaleStatus status);

    @Query(
            """
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.sellerProfile
            LEFT JOIN FETCH s.store
            WHERE s.store.id = :storeId
              AND s.saleDate >= :from
              AND s.saleDate < :to
              AND s.status IN (br.com.systemcommerce.sale.entity.Sale.SaleStatus.CONFIRMED,
                               br.com.systemcommerce.sale.entity.Sale.SaleStatus.PAID)
              AND s.sellerProfile IS NOT NULL
            ORDER BY s.saleDate
            """)
    List<Sale> findForCommissionCalculation(
            @Param("storeId") UUID storeId, @Param("from") Instant from, @Param("to") Instant to);
}
