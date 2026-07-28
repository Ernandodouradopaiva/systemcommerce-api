package br.com.systemcommerce.dashboard.dto;

import java.math.BigDecimal;

public record MoneyCountMetric(BigDecimal totalAmount, long count) {}
