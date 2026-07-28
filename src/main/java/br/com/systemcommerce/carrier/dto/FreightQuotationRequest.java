package br.com.systemcommerce.carrier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record FreightQuotationRequest(
        @NotNull UUID organizationId,
        UUID storeId,
        UUID carrierId,
        UUID freightModeId,
        UUID salesOrderId,
        UUID quoteId,
        String zipCode,
        BigDecimal weight,
        BigDecimal volume,
        BigDecimal orderAmount,
        @DecimalMin(value = "0.00", message = "Valor de frete não pode ser negativo") BigDecimal manualOverrideAmount,
        String notes) {}
