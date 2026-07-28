package br.com.systemcommerce.finance.incomestatement.repository;

import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementLayout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncomeStatementLayoutRepository extends JpaRepository<IncomeStatementLayout, UUID> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    Optional<IncomeStatementLayout> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    List<IncomeStatementLayout> findByOrganizationIdAndActiveTrueOrderByNameAsc(UUID organizationId);

    @Query("select l from IncomeStatementLayout l left join fetch l.lines where l.id = :id")
    Optional<IncomeStatementLayout> findDetailedById(@Param("id") UUID id);
}
