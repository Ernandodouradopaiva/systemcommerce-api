package br.com.systemcommerce.fiscal.taxation.engine.dto;

import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaxCalculationResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID establishmentId,
        boolean simulation,
        String operationCode,
        LocalDate issuedOn,
        TaxCalculation.CalculationStatus status,
        BigDecimal totalProducts,
        BigDecimal totalTax,
        String currency,
        String traceSummary,
        List<TaxCalculationItemResponse> items,
        List<TaxCalculationTraceResponse> traces,
        Instant createdAt) {}
