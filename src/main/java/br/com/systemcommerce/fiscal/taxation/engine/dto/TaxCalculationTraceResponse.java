package br.com.systemcommerce.fiscal.taxation.engine.dto;

import java.util.UUID;

public record TaxCalculationTraceResponse(
        UUID id, Integer stepOrder, String message, UUID ruleId, UUID itemId, String detailJson) {}
