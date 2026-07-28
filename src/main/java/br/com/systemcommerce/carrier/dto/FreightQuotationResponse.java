package br.com.systemcommerce.carrier.dto;

import br.com.systemcommerce.carrier.entity.FreightQuotation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FreightQuotationResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID carrierId,
        String carrierName,
        UUID freightModeId,
        String freightModeName,
        UUID salesOrderId,
        UUID quoteId,
        String zipCode,
        BigDecimal weight,
        BigDecimal volume,
        BigDecimal orderAmount,
        BigDecimal calculatedAmount,
        boolean manualOverride,
        BigDecimal overrideAmount,
        FreightQuotation.Source source,
        Instant calculatedAt,
        UUID calculatedBy,
        String notes) {}
