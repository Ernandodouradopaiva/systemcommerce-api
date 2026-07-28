package br.com.systemcommerce.commission.dto;

import br.com.systemcommerce.commission.entity.CommissionCalculation.CalculationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionCalculationResponse(
        UUID id,
        UUID saleId,
        String saleNumber,
        UUID saleItemId,
        UUID sellerProfileId,
        String sellerCode,
        UUID storeId,
        UUID policyId,
        String policyCode,
        Integer policyVersion,
        BigDecimal baseAmount,
        BigDecimal commissionAmount,
        CalculationStatus status,
        Instant calculatedAt,
        Instant createdAt,
        Long version) {}
