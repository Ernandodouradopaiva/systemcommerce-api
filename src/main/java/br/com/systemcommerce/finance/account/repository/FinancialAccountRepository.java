package br.com.systemcommerce.finance.account.repository;

import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialAccountRepository
        extends JpaRepository<FinancialAccount, UUID>, JpaSpecificationExecutor<FinancialAccount> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    List<FinancialAccount> findByOrganizationIdOrderBySortOrderAscCodeAsc(UUID organizationId);

    List<FinancialAccount> findByOrganizationIdAndAcceptsPostingTrueAndStatusOrderByCodeAsc(
            UUID organizationId, FinancialAccount.AccountStatus status);

    @Query("select a from FinancialAccount a left join fetch a.parent left join fetch a.organization where a.id = :id")
    Optional<FinancialAccount> findDetailedById(@Param("id") UUID id);

    long countByParentId(UUID parentId);
}
