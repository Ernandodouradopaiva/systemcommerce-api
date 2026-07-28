package br.com.systemcommerce.production.dto;

import br.com.systemcommerce.production.entity.BillOfMaterialsStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillOfMaterialsCreateRequest(
        @NotNull UUID organizationId,
        @NotNull UUID finishedProductId,
        @NotBlank String code,
        @NotBlank String name,
        Integer versionNumber,
        String notes,
        @NotEmpty @Valid List<BomItemRequest> items) {

    public record BomItemRequest(
            @NotNull UUID componentProductId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            @NotNull Integer lineNumber,
            String unitCode,
            BigDecimal scrapPercent) {}
}
