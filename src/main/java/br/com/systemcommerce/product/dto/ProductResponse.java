package br.com.systemcommerce.product.dto;

import br.com.systemcommerce.product.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String internalCode,
        String sku,
        String barcode,
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        String unitOfMeasure,
        BigDecimal costPrice,
        BigDecimal salePrice,
        BigDecimal minStock,
        BigDecimal currentStock,
        Boolean stockBelowMinimum,
        Boolean allowNegativeStock,
        Product.ProductStatus status,
        Boolean active,
        String imageUrl,
        UUID brandId,
        String brandName,
        UUID manufacturerId,
        String manufacturerName,
        UUID productLineId,
        String productLineName,
        Instant createdAt,
        Instant updatedAt) {}
