package br.com.systemcommerce.fiscal.party.dto;

import br.com.systemcommerce.fiscal.party.PartyType;
import java.time.Instant;
import java.util.UUID;

public record PartyFiscalHistoryResponse(
        UUID id,
        PartyType partyType,
        UUID partyId,
        UUID profileId,
        Instant changedAt,
        UUID changedBy,
        String changeType,
        String snapshotJson) {}
