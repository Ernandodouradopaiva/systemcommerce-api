package br.com.systemcommerce.fiscal.transmission.repository;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.transmission.entity.FiscalEndpointRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalEndpointRegistryRepository extends JpaRepository<FiscalEndpointRegistry, UUID> {

    Optional<FiscalEndpointRegistry> findByUfAndModelAndEnvironmentAndServiceNameAndActiveTrue(
            String uf, String model, String environment, String serviceName);

    List<FiscalEndpointRegistry> findByUfAndModelAndEnvironmentAndActiveTrue(
            String uf, String model, String environment);
}
