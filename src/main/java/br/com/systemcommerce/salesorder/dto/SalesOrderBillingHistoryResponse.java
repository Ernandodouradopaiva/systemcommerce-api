package br.com.systemcommerce.salesorder.dto;

import java.time.Instant;
import java.util.UUID;

public record SalesOrderBillingHistoryResponse(
        UUID id,
        UUID salesOrderId,
        UUID saleId,
        String eventType,
        String notes,
        Instant occurredAt,
        UUID performedBy) {}
