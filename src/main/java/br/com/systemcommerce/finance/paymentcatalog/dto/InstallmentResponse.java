package br.com.systemcommerce.finance.paymentcatalog.dto;

import java.math.BigDecimal;

public record InstallmentResponse(Integer sequenceNo, Integer daysOffset, BigDecimal percentage) {}
