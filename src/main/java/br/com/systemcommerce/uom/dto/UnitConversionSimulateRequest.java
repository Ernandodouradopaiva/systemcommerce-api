package br.com.systemcommerce.uom.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UnitConversionSimulateRequest(
        @NotNull(message = "unidade de origem é obrigatória") UUID fromUnitId,
        @NotNull(message = "unidade de destino é obrigatória") UUID toUnitId,
        @NotNull(message = "quantidade é obrigatória") BigDecimal quantity) {}
