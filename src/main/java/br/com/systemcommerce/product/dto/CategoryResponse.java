package br.com.systemcommerce.product.dto;

import br.com.systemcommerce.product.entity.Category;
import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        UUID parentId,
        String parentName,
        Category.CategoryStatus status,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
