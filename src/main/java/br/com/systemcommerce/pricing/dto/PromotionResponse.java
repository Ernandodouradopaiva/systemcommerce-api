package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.Promotion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PromotionResponse(
        UUID id,
        UUID organizationId,
        String organizationCode,
        String code,
        String name,
        String description,
        PriceChannel channel,
        Promotion.Status status,
        Integer priority,
        Instant validFrom,
        Instant validTo,
        List<UUID> storeIds,
        List<String> storeCodes,
        Promotion.PromotionType promotionType,
        boolean stackable,
        BigDecimal minOrderAmount,
        UUID brandId,
        UUID categoryId,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
