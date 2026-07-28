package br.com.systemcommerce.fiscal.party.dto;

import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PartyFiscalProfileUpdateRequest(
        @NotNull TaxpayerIndicator taxpayerIndicator,
        @Size(max = 30) String stateRegistration,
        @Size(max = 30) String municipalRegistration,
        @Size(max = 20) String suframa,
        Boolean finalConsumer,
        Boolean ruralProducer,
        Boolean foreignParty,
        @Size(max = 10) String countryCode,
        @Size(max = 7) String ibgeCityCode,
        @Size(max = 200) String fiscalEmail,
        @Size(max = 40) String taxRegime,
        String retentionFlagsJson,
        @NotNull LocalDate validFrom,
        LocalDate validUntil) {}
