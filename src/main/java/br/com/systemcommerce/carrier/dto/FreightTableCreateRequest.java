package br.com.systemcommerce.carrier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FreightTableCreateRequest(
        @NotNull UUID organizationId,
        UUID carrierId,
        UUID freightModeId,
        @NotBlank String name,
        LocalDate validFrom,
        LocalDate validUntil,
        @NotEmpty @Valid List<FreightRegionRequest> regions) {}
