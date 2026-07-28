package br.com.systemcommerce.catalog.dto;

import br.com.systemcommerce.catalog.entity.ProductLine;
import java.time.Instant;
import java.util.UUID;

public record ProductLineResponse(
        UUID id,
        UUID organizationId,
        UUID brandId,
        String brandName,
        String code,
        String name,
        String description,
        ProductLine.ProductLineStatus status,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
