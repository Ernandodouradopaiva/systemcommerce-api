package br.com.systemcommerce.carrier.dto;

import br.com.systemcommerce.carrier.entity.FreightMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FreightModeRequest(
        UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull FreightMode.ModeType modeType) {}
