package br.com.systemcommerce.reservation.repository;

import br.com.systemcommerce.reservation.entity.StockReservation;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReservationRepository
        extends JpaRepository<StockReservation, UUID>, JpaSpecificationExecutor<StockReservation> {

    @Query(
            """
            SELECT DISTINCT r FROM StockReservation r
            LEFT JOIN FETCH r.items i
            LEFT JOIN FETCH i.product
            LEFT JOIN FETCH r.store
            LEFT JOIN FETCH r.warehouse
            LEFT JOIN FETCH r.organization
            WHERE r.id = :id
            """)
    Optional<StockReservation> findDetailedById(@Param("id") UUID id);

    Optional<StockReservation> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query(
            """
            SELECT DISTINCT r FROM StockReservation r
            LEFT JOIN FETCH r.items i
            LEFT JOIN FETCH i.product
            WHERE r.originType = :originType AND r.originId = :originId
              AND r.status IN :statuses
            ORDER BY r.createdAt DESC
            """)
    List<StockReservation> findByOriginAndStatusIn(
            @Param("originType") StockReservation.OriginType originType,
            @Param("originId") UUID originId,
            @Param("statuses") Collection<StockReservation.ReservationStatus> statuses);

    @Query(
            """
            SELECT DISTINCT r FROM StockReservation r
            LEFT JOIN FETCH r.items i
            WHERE r.status IN ('ACTIVE', 'PARTIALLY_CONSUMED')
              AND r.expiresAt IS NOT NULL AND r.expiresAt < :now
            """)
    List<StockReservation> findActivePastDue(@Param("now") Instant now);

    @Query("SELECT COUNT(r) FROM StockReservation r WHERE r.reservationNumber LIKE CONCAT(:prefix, '%')")
    long countByReservationNumberPrefix(@Param("prefix") String prefix);
}
