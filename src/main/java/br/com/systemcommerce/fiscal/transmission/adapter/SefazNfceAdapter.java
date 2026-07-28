package br.com.systemcommerce.fiscal.transmission.adapter;

import br.com.systemcommerce.fiscal.config.FiscalProperties;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.transmission.service.FiscalCircuitBreaker;
import br.com.systemcommerce.fiscal.transmission.service.FiscalEndpointRegistryService;
import org.springframework.stereotype.Component;

@Component
public class SefazNfceAdapter extends AbstractSefazStubAdapter {

    public SefazNfceAdapter(
            FiscalProperties fiscalProperties,
            FiscalEndpointRegistryService endpointRegistry,
            FiscalCircuitBreaker circuitBreaker,
            FiscalEstablishmentRepository establishmentRepository) {
        super(fiscalProperties, endpointRegistry, circuitBreaker, establishmentRepository);
    }

    @Override
    protected String modelCode() {
        return "65";
    }
}
