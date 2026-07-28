package br.com.systemcommerce.finance.reversal.repository;

import br.com.systemcommerce.finance.reversal.entity.FinancialReversal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialReversalRepository
        extends JpaRepository<FinancialReversal, UUID>, JpaSpecificationExecutor<FinancialReversal> {

    Optional<FinancialReversal> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    boolean existsBySourceTypeAndSourceDocumentId(
            FinancialReversal.SourceType sourceType, UUID sourceDocumentId);

    @Query(
            """
            select r from FinancialReversal r
            left join fetch r.items
            left join fetch r.organization
            where r.id = :id
            """)
    Optional<FinancialReversal> findDetailedById(@Param("id") UUID id);
}
