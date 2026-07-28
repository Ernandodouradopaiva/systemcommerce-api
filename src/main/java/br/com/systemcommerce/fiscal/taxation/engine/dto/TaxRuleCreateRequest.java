package br.com.systemcommerce.fiscal.taxation.engine.dto;

import br.com.systemcommerce.fiscal.taxation.engine.TaxKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaxRuleCreateRequest(
        UUID organizationId,
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String description,
        @NotNull TaxKind taxKind,
        @NotNull Integer priority,
        @NotNull LocalDate validFrom,
        LocalDate validUntil,
        @Size(max = 40) String versionCode,
        @Valid List<TaxRuleConditionRequest> conditions,
        @Valid List<TaxRuleResultRequest> results) {}
