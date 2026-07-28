package br.com.systemcommerce.finance.paymentcatalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DueDateItem(Integer sequenceNo, LocalDate dueDate, BigDecimal percentage, BigDecimal amount) {}
