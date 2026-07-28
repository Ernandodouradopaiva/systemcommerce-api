package br.com.systemcommerce.finance.incomestatement.repository;

import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementExecution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncomeStatementExecutionRepository extends JpaRepository<IncomeStatementExecution, UUID> {

    List<IncomeStatementExecution> findByOrganizationIdOrderByExecutedAtDesc(UUID organizationId);

    @Query("select e from IncomeStatementExecution e left join fetch e.lines where e.id = :id")
    Optional<IncomeStatementExecution> findDetailedById(@Param("id") UUID id);
}
