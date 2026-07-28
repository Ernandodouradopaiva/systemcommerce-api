package br.com.systemcommerce.finance.costcenter.dto;

import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CostCenterCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        UUID parentId,
        UUID storeId,
        UUID responsibleUserId,
        Boolean acceptsPosting,
        LocalDate validFrom,
        LocalDate validUntil,
        Integer sortOrder) {}
