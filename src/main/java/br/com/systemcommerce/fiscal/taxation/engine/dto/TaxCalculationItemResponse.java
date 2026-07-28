package br.com.systemcommerce.fiscal.taxation.engine.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxCalculationItemResponse(
        UUID id,
        Integer lineNumber,
        UUID productId,
        String ncm,
        String cest,
        String originCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String taxBreakdownJson,
        String selectedRuleCodes) {}
