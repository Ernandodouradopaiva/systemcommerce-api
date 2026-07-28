package br.com.systemcommerce.fiscal.versioning.repository;

import br.com.systemcommerce.fiscal.versioning.entity.FiscalTaxRuleSetVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalTaxRuleSetVersionRepository extends JpaRepository<FiscalTaxRuleSetVersion, UUID> {
    Optional<FiscalTaxRuleSetVersion> findByCode(String code);
}
