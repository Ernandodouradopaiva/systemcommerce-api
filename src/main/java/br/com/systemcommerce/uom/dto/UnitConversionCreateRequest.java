package br.com.systemcommerce.uom.dto;

import br.com.systemcommerce.uom.entity.RoundingModeOption;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UnitConversionCreateRequest(
        UUID organizationId,
        @NotNull(message = "unidade de origem é obrigatória") UUID fromUnitId,
        @NotNull(message = "unidade de destino é obrigatória") UUID toUnitId,
        @NotNull(message = "fator é obrigatório") @DecimalMin(value = "0.0000000001", message = "fator deve ser maior que zero")
                BigDecimal factor,
        RoundingModeOption roundingMode) {}
