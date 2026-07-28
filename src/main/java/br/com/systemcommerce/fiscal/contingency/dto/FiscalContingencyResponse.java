package br.com.systemcommerce.fiscal.contingency.dto;

import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency.Mode;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency.Status;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.time.Instant;
import java.util.UUID;

public record FiscalContingencyResponse(
        UUID id,
        UUID establishmentId,
        String model,
        FiscalEstablishment.FiscalEnvironment environment,
        Mode mode,
        Status status,
        String reason,
        Instant startedAt,
        UUID startedBy,
        Instant endedAt,
        UUID endedBy,
        String uf) {}
