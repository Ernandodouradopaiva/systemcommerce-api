package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.entity.PriceTableScopeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PriceTableResponse(
        UUID id,
        String code,
        String name,
        String description,
        PriceTable.Status status,
        Integer priority,
        PriceChannel channel,
        PriceTableScopeType scopeType,
        UUID storeGroupId,
        String storeGroupCode,
        Instant validFrom,
        Instant validTo,
        List<UUID> storeIds,
        List<String> storeCodes,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
