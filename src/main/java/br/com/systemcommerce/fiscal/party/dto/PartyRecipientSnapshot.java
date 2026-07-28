package br.com.systemcommerce.fiscal.party.dto;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import java.util.UUID;

public record PartyRecipientSnapshot(
        UUID profileId,
        PartyType partyType,
        UUID partyId,
        TaxpayerIndicator taxpayerIndicator,
        String stateRegistration,
        String municipalRegistration,
        String suframa,
        boolean finalConsumer,
        boolean ruralProducer,
        boolean foreignParty,
        String countryCode,
        String ibgeCityCode,
        String fiscalEmail,
        String taxRegime) {}
