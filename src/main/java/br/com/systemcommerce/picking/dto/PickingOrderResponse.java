package br.com.systemcommerce.picking.dto;

import br.com.systemcommerce.picking.entity.PickingOrder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PickingOrderResponse(
        UUID id,
        String pickingNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        UUID salesOrderId,
        String salesOrderNumber,
        UUID stockReservationId,
        PickingOrder.PickingOrderStatus status,
        UUID assignedToUserId,
        String assignedToUserName,
        Instant startedAt,
        Instant completedAt,
        String notes,
        List<PickingOrderItemResponse> items,
        boolean canStart,
        boolean canComplete,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
