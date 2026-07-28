package br.com.systemcommerce.production.dto;

import br.com.systemcommerce.production.entity.BillOfMaterialsStatus;
import java.util.List;
import java.util.UUID;

public record BillOfMaterialsResponse(
        UUID id,
        UUID organizationId,
        UUID finishedProductId,
        String finishedProductSku,
        String code,
        String name,
        Integer versionNumber,
        BillOfMaterialsStatus status,
        String notes,
        List<BillOfMaterialsItemResponse> items) {}
