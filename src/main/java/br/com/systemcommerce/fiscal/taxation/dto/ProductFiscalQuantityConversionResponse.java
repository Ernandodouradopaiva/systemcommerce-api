package br.com.systemcommerce.fiscal.taxation.dto;

import java.math.BigDecimal;

public record ProductFiscalQuantityConversionResponse(BigDecimal commercialQty, BigDecimal taxableQty) {}
