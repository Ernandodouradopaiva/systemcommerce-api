package br.com.systemcommerce.finance.advance.repository;

import br.com.systemcommerce.finance.advance.entity.SupplierAdvance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierAdvanceRepository
        extends JpaRepository<SupplierAdvance, UUID>, JpaSpecificationExecutor<SupplierAdvance> {
    Optional<SupplierAdvance> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("""
            select a from SupplierAdvance a
            join fetch a.organization join fetch a.supplier join fetch a.holder
            left join fetch a.store where a.id = :id
            """)
    Optional<SupplierAdvance> findDetailedById(@Param("id") UUID id);
}
