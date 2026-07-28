package br.com.systemcommerce.carrier.repository;

import br.com.systemcommerce.carrier.entity.FreightMode;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FreightModeRepository extends JpaRepository<FreightMode, UUID>, JpaSpecificationExecutor<FreightMode> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);
}
