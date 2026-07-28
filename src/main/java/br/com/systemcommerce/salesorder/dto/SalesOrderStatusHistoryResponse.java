package br.com.systemcommerce.salesorder.dto;

import br.com.systemcommerce.salesorder.entity.SalesOrder;
import java.time.Instant;
import java.util.UUID;

public record SalesOrderStatusHistoryResponse(
        UUID id,
        SalesOrder.SalesOrderStatus fromStatus,
        SalesOrder.SalesOrderStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
