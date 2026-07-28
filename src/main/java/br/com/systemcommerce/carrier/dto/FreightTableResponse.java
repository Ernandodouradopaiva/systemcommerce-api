package br.com.systemcommerce.carrier.dto;

import br.com.systemcommerce.carrier.entity.FreightTable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FreightTableResponse(
        UUID id,
        UUID organizationId,
        UUID carrierId,
        String carrierName,
        UUID freightModeId,
        String freightModeName,
        String name,
        FreightTable.FreightTableStatus status,
        LocalDate validFrom,
        LocalDate validUntil,
        List<FreightRegionResponse> regions) {}
