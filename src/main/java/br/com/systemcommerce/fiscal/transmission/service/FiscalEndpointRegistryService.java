package br.com.systemcommerce.fiscal.transmission.service;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.transmission.entity.FiscalEndpointRegistry;
import br.com.systemcommerce.fiscal.transmission.repository.FiscalEndpointRegistryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalEndpointRegistryService {

    private final FiscalEndpointRegistryRepository repository;

    @Transactional(readOnly = true)
    public String resolveUrl(String uf, String model, FiscalEstablishment.FiscalEnvironment environment, String serviceName) {
        return repository
                .findByUfAndModelAndEnvironmentAndServiceNameAndActiveTrue(
                        uf, model, environment.name(), serviceName)
                .map(FiscalEndpointRegistry::getUrl)
                .orElse("https://stub.sefaz.local/" + uf + "/" + model + "/" + serviceName);
    }

    @Transactional(readOnly = true)
    public int resolveTimeoutMs(String uf, String model, FiscalEstablishment.FiscalEnvironment environment, String serviceName) {
        return repository
                .findByUfAndModelAndEnvironmentAndServiceNameAndActiveTrue(
                        uf, model, environment.name(), serviceName)
                .map(FiscalEndpointRegistry::getTimeoutMs)
                .orElse(30000);
    }

    @Transactional(readOnly = true)
    public List<FiscalEndpointRegistry> listByUfModelEnv(String uf, String model, String environment) {
        return repository.findByUfAndModelAndEnvironmentAndActiveTrue(uf, model, environment);
    }
}
