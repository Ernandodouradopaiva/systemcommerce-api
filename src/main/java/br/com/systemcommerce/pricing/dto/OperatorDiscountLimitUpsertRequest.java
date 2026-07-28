package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record OperatorDiscountLimitUpsertRequest(
        @NotNull(message = "perfil é obrigatório") UUID roleId,
        @NotNull(message = "percentual máximo é obrigatório")
                @DecimalMin(value = "0.0000", message = "percentual máximo não pode ser negativo")
                @DecimalMax(value = "100.0000", message = "percentual máximo não pode ultrapassar 100")
                BigDecimal maxPercent,
        @DecimalMin(value = "0.00", message = "valor máximo não pode ser negativo") BigDecimal maxAmount) {}
