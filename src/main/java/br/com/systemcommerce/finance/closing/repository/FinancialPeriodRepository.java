package br.com.systemcommerce.finance.closing.repository;

import br.com.systemcommerce.finance.closing.entity.FinancialPeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, UUID> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    List<FinancialPeriod> findByOrganizationIdOrderByStartDateDesc(UUID organizationId);

    @Query("""
            select p from FinancialPeriod p
            where p.organization.id = :organizationId
              and p.status = 'CLOSED'
              and p.startDate <= :date and p.endDate >= :date
              and (p.store is null or p.store.id = :storeId)
            """)
    List<FinancialPeriod> findClosedCovering(
            @Param("organizationId") UUID organizationId,
            @Param("storeId") UUID storeId,
            @Param("date") LocalDate date);

    @Query("""
            select p from FinancialPeriod p
            where p.organization.id = :organizationId
              and p.status = 'CLOSED'
              and p.store is null
              and p.startDate <= :date and p.endDate >= :date
            """)
    List<FinancialPeriod> findClosedOrgCovering(
            @Param("organizationId") UUID organizationId, @Param("date") LocalDate date);

    @Query("select p from FinancialPeriod p left join fetch p.closings where p.id = :id")
    Optional<FinancialPeriod> findDetailedById(@Param("id") UUID id);
}
