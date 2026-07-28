package br.com.systemcommerce.fiscal.contingency.dto;

import br.com.systemcommerce.fiscal.contingency.entity.ContingencyActivation.TriggerKind;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency.Mode;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ContingencyActivateRequest(
        @NotNull UUID establishmentId,
        @NotBlank @Size(max = 10) String model,
        @NotNull FiscalEstablishment.FiscalEnvironment environment,
        @NotNull Mode mode,
        @NotBlank @Size(max = 500) String reason,
        TriggerKind triggerKind,
        String detailJson) {}
