package br.com.systemcommerce.finance.entry.repository;

import br.com.systemcommerce.finance.entry.entity.FinancialEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialEntryRepository
        extends JpaRepository<FinancialEntry, UUID>, JpaSpecificationExecutor<FinancialEntry> {

    Optional<FinancialEntry> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query(
            """
            select e from FinancialEntry e
            left join fetch e.holder
            left join fetch e.financialCategory c
            left join fetch c.financialAccount
            left join fetch e.organization
            where e.id = :id
            """)
    Optional<FinancialEntry> findDetailedById(@Param("id") UUID id);
}
