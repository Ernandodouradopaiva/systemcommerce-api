package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.StoreGroup;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoreGroupResponse(
        UUID id,
        UUID organizationId,
        String organizationCode,
        String code,
        String name,
        String description,
        StoreGroup.Status status,
        List<UUID> storeIds,
        List<String> storeCodes,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
