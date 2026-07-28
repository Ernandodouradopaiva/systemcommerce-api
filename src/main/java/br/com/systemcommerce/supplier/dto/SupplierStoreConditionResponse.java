package br.com.systemcommerce.supplier.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupplierStoreConditionResponse(
        UUID id,
        UUID supplierId,
        UUID storeId,
        String storeCode,
        String storeName,
        String notes,
        Integer paymentTermDays,
        String paymentCondition,
        BigDecimal minOrderAmount,
        Integer averageLeadTimeDays,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
