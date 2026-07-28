package br.com.systemcommerce.finance.paymentcatalog.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CalculateDueDatesRequest(@NotNull LocalDate baseDate, BigDecimal amount) {}
