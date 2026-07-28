package br.com.systemcommerce.uom.dto;

import br.com.systemcommerce.uom.entity.RoundingModeOption;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UnitConversionResponse(
        UUID id,
        UUID fromUnitId,
        String fromUnitCode,
        UUID toUnitId,
        String toUnitCode,
        BigDecimal factor,
        RoundingModeOption roundingMode,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
