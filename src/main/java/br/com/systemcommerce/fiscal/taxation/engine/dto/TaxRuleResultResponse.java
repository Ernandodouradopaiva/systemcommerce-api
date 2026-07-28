package br.com.systemcommerce.fiscal.taxation.engine.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxRuleResultResponse(UUID id, String resultKey, String resultValue, BigDecimal numericValue) {}
