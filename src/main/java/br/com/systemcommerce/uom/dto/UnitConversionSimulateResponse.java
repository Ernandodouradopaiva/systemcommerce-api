package br.com.systemcommerce.uom.dto;

import java.math.BigDecimal;

public record UnitConversionSimulateResponse(BigDecimal inputQuantity, BigDecimal convertedQuantity) {}
