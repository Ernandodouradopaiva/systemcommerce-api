package br.com.systemcommerce.finance.advance.repository;

import br.com.systemcommerce.finance.advance.entity.CustomerAdvance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAdvanceRepository
        extends JpaRepository<CustomerAdvance, UUID>, JpaSpecificationExecutor<CustomerAdvance> {
    Optional<CustomerAdvance> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("""
            select a from CustomerAdvance a
            join fetch a.organization join fetch a.customer join fetch a.holder
            left join fetch a.store where a.id = :id
            """)
    Optional<CustomerAdvance> findDetailedById(@Param("id") UUID id);
}
