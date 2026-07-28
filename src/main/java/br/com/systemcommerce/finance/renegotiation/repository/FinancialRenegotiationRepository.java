package br.com.systemcommerce.finance.renegotiation.repository;

import br.com.systemcommerce.finance.renegotiation.entity.FinancialRenegotiation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialRenegotiationRepository
        extends JpaRepository<FinancialRenegotiation, UUID>, JpaSpecificationExecutor<FinancialRenegotiation> {

    Optional<FinancialRenegotiation> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query(
            """
            select r from FinancialRenegotiation r
            left join fetch r.organization
            where r.id = :id
            """)
    Optional<FinancialRenegotiation> findDetailedById(@Param("id") UUID id);
}
