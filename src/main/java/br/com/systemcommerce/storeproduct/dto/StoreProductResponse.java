package br.com.systemcommerce.storeproduct.dto;

import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StoreProductResponse(
        UUID id,
        UUID storeId,
        String storeCode,
        String storeName,
        UUID productId,
        String productSku,
        String productName,
        StoreProduct.StoreProductStatus status,
        boolean allowsSale,
        boolean allowsPosSale,
        boolean allowsErpSale,
        String localInternalCode,
        String localBarcode,
        BigDecimal localDefaultPrice,
        BigDecimal localMinStock,
        BigDecimal localMaxStock,
        boolean allowNegativeStock,
        String physicalLocation,
        String aisle,
        String shelf,
        String displayPosition,
        LocalDate commercializationStart,
        LocalDate commercializationEnd,
        String blockReason,
        Boolean active,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
