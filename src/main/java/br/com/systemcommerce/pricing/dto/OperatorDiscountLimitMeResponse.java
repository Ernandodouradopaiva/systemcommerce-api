package br.com.systemcommerce.pricing.dto;

import java.math.BigDecimal;

public record OperatorDiscountLimitMeResponse(BigDecimal maxPercent, BigDecimal maxAmount, String roleCode) {}
