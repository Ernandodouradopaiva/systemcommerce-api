package br.com.systemcommerce.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PosPeriodRow(
        LocalDate periodDate,
        Integer hour,
        long saleCount,
        BigDecimal totalAmount,
        BigDecimal averageTicket,
        BigDecimal itemQuantity) {}
