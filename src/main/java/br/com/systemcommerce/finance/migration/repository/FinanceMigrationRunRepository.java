package br.com.systemcommerce.finance.migration.repository;

import br.com.systemcommerce.finance.migration.entity.FinanceMigrationRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceMigrationRunRepository extends JpaRepository<FinanceMigrationRun, UUID> {
    List<FinanceMigrationRun> findByOrganizationIdOrderByStartedAtDesc(UUID organizationId);
}
