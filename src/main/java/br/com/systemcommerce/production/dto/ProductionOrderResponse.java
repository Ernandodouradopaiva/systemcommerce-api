package br.com.systemcommerce.production.dto;

import br.com.systemcommerce.production.entity.ProductionOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductionOrderResponse(
        UUID id,
        String orderNumber,
        ProductionOrderStatus status,
        UUID storeId,
        UUID warehouseId,
        UUID billOfMaterialsId,
        UUID finishedProductId,
        String finishedProductSku,
        BigDecimal quantityPlanned,
        BigDecimal quantityCompleted,
        Instant plannedStart,
        Instant plannedEnd,
        Instant startedAt,
        Instant completedAt,
        BigDecimal unitCost,
        BigDecimal totalCost,
        String notes) {}
