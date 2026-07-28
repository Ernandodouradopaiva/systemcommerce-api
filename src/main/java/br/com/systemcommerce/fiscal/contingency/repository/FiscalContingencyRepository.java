package br.com.systemcommerce.fiscal.contingency.repository;

import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency.Status;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalContingencyRepository extends JpaRepository<FiscalContingency, UUID> {

    Optional<FiscalContingency> findFirstByEstablishmentAndModelAndEnvironmentAndStatusAndActiveTrue(
            FiscalEstablishment establishment,
            String model,
            FiscalEstablishment.FiscalEnvironment environment,
            Status status);

    List<FiscalContingency> findByEstablishmentIdAndStatusAndActiveTrueOrderByStartedAtDesc(
            UUID establishmentId, Status status);
}
