package br.com.systemcommerce.uom.dto;

import br.com.systemcommerce.uom.entity.RoundingModeOption;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUnitResponse(
        UUID id,
        UUID productId,
        UUID stockUnitId,
        String stockUnitCode,
        UUID purchaseUnitId,
        String purchaseUnitCode,
        UUID salesUnitId,
        String salesUnitCode,
        BigDecimal purchaseToStockFactor,
        BigDecimal salesToStockFactor,
        RoundingModeOption roundingMode,
        Boolean active) {}
