package br.com.systemcommerce.finance.bank.repository;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialAccountHolderRepository
        extends JpaRepository<FinancialAccountHolder, UUID>, JpaSpecificationExecutor<FinancialAccountHolder> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    List<FinancialAccountHolder> findByOrganizationIdAndActiveTrueOrderByNameAsc(UUID organizationId);

    @Query(
            "select h from FinancialAccountHolder h left join fetch h.organization left join fetch h.store where h.id = :id")
    Optional<FinancialAccountHolder> findDetailedById(@Param("id") UUID id);
}
