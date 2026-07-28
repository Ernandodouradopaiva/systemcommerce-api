package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.ProductPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductPriceResponse(
        UUID id,
        UUID priceTableId,
        String priceTableCode,
        UUID productId,
        String productSku,
        String productName,
        ProductPrice.PriceType priceType,
        BigDecimal unitPrice,
        BigDecimal minQuantity,
        Integer priority,
        ProductPrice.Status status,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
