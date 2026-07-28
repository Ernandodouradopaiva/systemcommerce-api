package br.com.systemcommerce.finance.payable.repository;

import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceGenerationSettingsRepository extends JpaRepository<FinanceGenerationSettings, UUID> {
    Optional<FinanceGenerationSettings> findByOrganizationId(UUID organizationId);
}