package br.com.systemcommerce.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Resultado de simulação (não persistido). */
public record CommissionSimulateLineResponse(
        UUID saleId,
        UUID saleItemId,
        UUID policyId,
        String policyCode,
        BigDecimal baseAmount,
        BigDecimal commissionAmount) {}
