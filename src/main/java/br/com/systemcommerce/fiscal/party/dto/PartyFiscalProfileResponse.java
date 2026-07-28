package br.com.systemcommerce.fiscal.party.dto;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import br.com.systemcommerce.fiscal.party.entity.PartyFiscalProfile;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PartyFiscalProfileResponse(
        UUID id,
        UUID organizationId,
        PartyType partyType,
        UUID partyId,
        UUID storeId,
        TaxpayerIndicator taxpayerIndicator,
        String stateRegistration,
        String municipalRegistration,
        String suframa,
        Boolean finalConsumer,
        Boolean ruralProducer,
        Boolean foreignParty,
        String countryCode,
        String ibgeCityCode,
        String fiscalEmail,
        String taxRegime,
        String retentionFlagsJson,
        PartyFiscalProfile.ProfileStatus status,
        boolean usable,
        LocalDate validFrom,
        LocalDate validUntil,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
