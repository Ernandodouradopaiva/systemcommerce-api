package br.com.systemcommerce.stockentry.dto;

import br.com.systemcommerce.stockentry.entity.StockEntryStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StockEntryResponse(
        UUID id,
        UUID organizationId,
        String number,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        String supplierName,
        String documentNumber,
        LocalDate entryDate,
        StockEntryStatus status,
        UUID responsibleUserId,
        String responsibleUserName,
        String notes,
        Instant confirmedAt,
        Instant cancelledAt,
        BigDecimal totalAmount,
        List<StockEntryItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {}
