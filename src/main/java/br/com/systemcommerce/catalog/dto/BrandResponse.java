package br.com.systemcommerce.catalog.dto;

import br.com.systemcommerce.catalog.entity.Brand;
import java.time.Instant;
import java.util.UUID;

public record BrandResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        String countryCode,
        String website,
        String logoUrl,
        Brand.BrandStatus status,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
