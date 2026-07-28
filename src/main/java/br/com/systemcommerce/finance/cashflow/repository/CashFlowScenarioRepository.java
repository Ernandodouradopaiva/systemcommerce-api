package br.com.systemcommerce.finance.cashflow.repository;

import br.com.systemcommerce.finance.cashflow.entity.CashFlowScenario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashFlowScenarioRepository extends JpaRepository<CashFlowScenario, UUID> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    List<CashFlowScenario> findByOrganizationIdAndActiveTrueOrderByNameAsc(UUID organizationId);

    Optional<CashFlowScenario> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
