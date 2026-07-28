package br.com.systemcommerce.uom.dto;

import br.com.systemcommerce.uom.entity.RoundingModeOption;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUnitUpsertRequest(
        @NotNull(message = "unidade de estoque é obrigatória") UUID stockUnitId,
        @NotNull(message = "unidade de compra é obrigatória") UUID purchaseUnitId,
        @NotNull(message = "unidade de venda é obrigatória") UUID salesUnitId,
        @DecimalMin(value = "0.0000000001", message = "fator deve ser maior que zero") BigDecimal purchaseToStockFactor,
        @DecimalMin(value = "0.0000000001", message = "fator deve ser maior que zero") BigDecimal salesToStockFactor,
        RoundingModeOption roundingMode) {}
