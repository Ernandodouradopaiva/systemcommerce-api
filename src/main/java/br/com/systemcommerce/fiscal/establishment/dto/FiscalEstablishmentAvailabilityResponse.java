package br.com.systemcommerce.fiscal.establishment.dto;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.util.List;

public record FiscalEstablishmentAvailabilityResponse(
        boolean available,
        boolean usable,
        FiscalEstablishment.FiscalEnvironment fiscalEnvironment,
        boolean allowsNfe,
        boolean allowsNfce,
        boolean nfeSeriesConfigured,
        boolean nfceSeriesConfigured,
        List<String> validationMessages) {}
