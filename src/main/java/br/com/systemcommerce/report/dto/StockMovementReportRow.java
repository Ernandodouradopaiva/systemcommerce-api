package br.com.systemcommerce.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovementReportRow(
        UUID id,
        String productSku,
        String productName,
        String type,
        BigDecimal quantity,
        BigDecimal previousQuantity,
        BigDecimal newQuantity,
        String referenceType,
        Instant createdAt) {}
