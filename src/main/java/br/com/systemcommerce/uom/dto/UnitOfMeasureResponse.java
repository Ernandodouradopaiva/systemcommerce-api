package br.com.systemcommerce.uom.dto;

import br.com.systemcommerce.uom.entity.UnitOfMeasure;
import java.time.Instant;
import java.util.UUID;

public record UnitOfMeasureResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        String symbol,
        Integer precisionScale,
        UnitOfMeasure.UomStatus status,
        Boolean systemUnit,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
