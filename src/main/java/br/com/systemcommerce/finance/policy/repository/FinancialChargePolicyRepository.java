package br.com.systemcommerce.finance.policy.repository;

import br.com.systemcommerce.finance.policy.entity.FinancialChargePolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialChargePolicyRepository
        extends JpaRepository<FinancialChargePolicy, UUID>, JpaSpecificationExecutor<FinancialChargePolicy> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    @Query(
            """
            select p from FinancialChargePolicy p
            left join fetch p.organization
            left join fetch p.store
            where p.id = :id
            """)
    Optional<FinancialChargePolicy> findDetailedById(@Param("id") UUID id);
}
