package br.com.systemcommerce.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionAdjustmentResponse(
        UUID id,
        UUID calculationId,
        BigDecimal amount,
        String reason,
        UUID createdBy) {}
