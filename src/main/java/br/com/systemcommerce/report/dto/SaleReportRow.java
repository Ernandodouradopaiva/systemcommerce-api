package br.com.systemcommerce.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleReportRow(
        UUID id,
        String saleNumber,
        Instant saleDate,
        String status,
        BigDecimal totalAmount,
        String customerName,
        String sellerName) {}
