package br.com.systemcommerce.finance.account.repository;

import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FinancialCategoryRepository
        extends JpaRepository<FinancialCategory, UUID>, JpaSpecificationExecutor<FinancialCategory> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    @Query(
            """
            select c from FinancialCategory c
            left join fetch c.financialAccount
            left join fetch c.organization
            where c.id = :id
            """)
    Optional<FinancialCategory> findDetailedById(@Param("id") UUID id);
}
