package br.com.systemcommerce.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DayAmountMetric(LocalDate day, BigDecimal totalAmount, long count) {}
