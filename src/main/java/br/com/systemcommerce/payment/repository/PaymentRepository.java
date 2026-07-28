package br.com.systemcommerce.payment.repository;

import br.com.systemcommerce.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query(
            """
            SELECT p FROM Payment p
            LEFT JOIN FETCH p.sale
            LEFT JOIN FETCH p.responsibleUser
            WHERE p.sale.id = :saleId
            ORDER BY p.createdAt ASC
            """)
    List<Payment> findBySaleIdOrderByCreatedAtAsc(@Param("saleId") UUID saleId);

    @Query(
            """
            SELECT p FROM Payment p
            LEFT JOIN FETCH p.sale
            LEFT JOIN FETCH p.responsibleUser
            LEFT JOIN FETCH p.cashSession
            WHERE p.id = :id
            """)
    Optional<Payment> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Query(
            """
            SELECT COALESCE(SUM(COALESCE(p.appliedAmount, p.amount)), 0)
            FROM Payment p
            WHERE p.sale.id = :saleId
              AND p.status = :status
            """)
    BigDecimal sumAmountBySaleIdAndStatus(
            @Param("saleId") UUID saleId, @Param("status") Payment.PaymentStatus status);

    default BigDecimal sumConfirmedAmountBySaleId(UUID saleId) {
        return sumAmountBySaleIdAndStatus(saleId, Payment.PaymentStatus.CONFIRMED);
    }

    @Query(
            """
            SELECT COUNT(p) > 0
            FROM Payment p
            WHERE p.sale.id = :saleId
              AND p.status = :status
            """)
    boolean existsBySaleIdAndStatus(
            @Param("saleId") UUID saleId, @Param("status") Payment.PaymentStatus status);

    default boolean hasConfirmedPayments(UUID saleId) {
        return existsBySaleIdAndStatus(saleId, Payment.PaymentStatus.CONFIRMED);
    }

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.cashSession.id = :sessionId AND p.status = :status
            """)
    BigDecimal sumAmountByCashSessionIdAndStatus(
            @Param("sessionId") UUID sessionId, @Param("status") Payment.PaymentStatus status);

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.cashSession.id = :sessionId
              AND p.status = :status
              AND p.method = :method
            """)
    BigDecimal sumAmountByCashSessionIdAndStatusAndMethod(
            @Param("sessionId") UUID sessionId,
            @Param("status") Payment.PaymentStatus status,
            @Param("method") Payment.PaymentMethod method);

    default BigDecimal sumConfirmedCashByCashSessionId(UUID sessionId) {
        return sumAmountByCashSessionIdAndStatusAndMethod(
                sessionId, Payment.PaymentStatus.CONFIRMED, Payment.PaymentMethod.CASH);
    }

    @Query(
            """
            SELECT p.method, COALESCE(SUM(COALESCE(p.appliedAmount, p.amount)), 0)
            FROM Payment p
            WHERE p.cashSession.id = :sessionId AND p.status = :status
            GROUP BY p.method
            ORDER BY p.method
            """)
    List<Object[]> sumGroupedByMethod(
            @Param("sessionId") UUID sessionId, @Param("status") Payment.PaymentStatus status);

    default List<Object[]> sumConfirmedGroupedByMethod(UUID sessionId) {
        return sumGroupedByMethod(sessionId, Payment.PaymentStatus.CONFIRMED);
    }

    boolean existsByCashSessionId(UUID cashSessionId);

    @Query(
            """
            SELECT p FROM Payment p
            JOIN FETCH p.sale s
            LEFT JOIN FETCH p.cashSession
            WHERE p.idempotencyKey = :key
            """)
    Optional<Payment> findByIdempotencyKey(@Param("key") String key);

    long countBySaleIdAndStatus(UUID saleId, Payment.PaymentStatus status);
}
