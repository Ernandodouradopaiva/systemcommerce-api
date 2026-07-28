package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderStatusHistoryResponse(
        UUID id,
        PurchaseOrder.PurchaseOrderStatus fromStatus,
        PurchaseOrder.PurchaseOrderStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
