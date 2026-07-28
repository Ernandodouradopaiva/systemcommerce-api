package br.com.systemcommerce.commission.repository;

import br.com.systemcommerce.commission.entity.SalesTarget;
import br.com.systemcommerce.commission.entity.SalesTarget.TargetStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesTargetRepository extends JpaRepository<SalesTarget, UUID> {

    Page<SalesTarget> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query(
            """
            SELECT t FROM SalesTarget t
            WHERE t.organization.id = :organizationId
              AND t.status = :status
              AND t.periodStart <= :periodEnd
              AND t.periodEnd >= :periodStart
              AND (:storeId IS NULL OR t.store IS NULL OR t.store.id = :storeId)
            """)
    List<SalesTarget> findOverlappingActive(
            @Param("organizationId") UUID organizationId,
            @Param("storeId") UUID storeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("status") TargetStatus status);
}
