package br.com.systemcommerce.supplier.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupplierProductResponse(
        UUID id,
        UUID supplierId,
        UUID productId,
        String productSku,
        String productName,
        String supplierSku,
        BigDecimal lastPurchasePrice,
        Integer leadTimeDays,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
