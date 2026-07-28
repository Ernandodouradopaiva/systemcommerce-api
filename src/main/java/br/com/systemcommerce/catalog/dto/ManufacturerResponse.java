package br.com.systemcommerce.catalog.dto;

import br.com.systemcommerce.catalog.entity.Manufacturer;
import java.time.Instant;
import java.util.UUID;

public record ManufacturerResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        String countryCode,
        String website,
        String logoUrl,
        Manufacturer.ManufacturerStatus status,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
