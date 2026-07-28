package br.com.systemcommerce.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductionOrderCreateRequest(
        @NotNull UUID organizationId,
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        @NotNull UUID billOfMaterialsId,
        @NotNull @DecimalMin("0.001") BigDecimal quantityPlanned,
        Instant plannedStart,
        Instant plannedEnd,
        String notes,
        String idempotencyKey) {}
