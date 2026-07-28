package br.com.systemcommerce.fiscal.operation.repository;

import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalOperationRepository
        extends JpaRepository<FiscalOperation, UUID>, JpaSpecificationExecutor<FiscalOperation> {

    Optional<FiscalOperation> findByOrganizationIdAndCode(UUID organizationId, String code);

    @Query(
            """
            select o from FiscalOperation o
            left join fetch o.organization
            left join fetch o.rules
            left join fetch o.storeAssignments sa
            left join fetch sa.store
            where o.id = :id
            """)
    Optional<FiscalOperation> findDetailedById(@Param("id") UUID id);

    List<FiscalOperation> findByOrganizationIdOrderByCode(UUID organizationId);
}
