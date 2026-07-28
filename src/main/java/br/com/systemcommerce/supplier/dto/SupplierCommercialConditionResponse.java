package br.com.systemcommerce.supplier.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupplierCommercialConditionResponse(
        UUID id,
        UUID supplierId,
        Integer paymentTermDays,
        String paymentCondition,
        String preferredCarrierName,
        BigDecimal minOrderAmount,
        Integer averageLeadTimeDays,
        String notes,
        Instant createdAt,
        Instant updatedAt) {}
