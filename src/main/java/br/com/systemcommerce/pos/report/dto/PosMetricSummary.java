package br.com.systemcommerce.pos.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PosMetricSummary(
        long saleCount,
        BigDecimal totalAmount,
        BigDecimal averageTicket,
        BigDecimal itemQuantity,
        BigDecimal discountAmount,
        BigDecimal avgItemsPerSale,
        Double avgServiceMinutes,
        Instant from,
        Instant toExclusive) {}
