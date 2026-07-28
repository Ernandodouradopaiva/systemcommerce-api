package br.com.systemcommerce.finance.costcenter.dto;

import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CostCenterResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        UUID parentId,
        UUID storeId,
        String storeCode,
        UUID responsibleUserId,
        boolean acceptsPosting,
        LocalDate validFrom,
        LocalDate validUntil,
        CostCenter.CostCenterStatus status,
        boolean usable,
        Integer sortOrder,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<CostCenterResponse> children) {}
