package br.com.systemcommerce.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryReportRow(
        UUID inventoryId,
        UUID productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal minStock,
        String unitOfMeasure) {}
