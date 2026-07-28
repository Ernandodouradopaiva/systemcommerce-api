package br.com.systemcommerce.shipment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSku,
        UUID salesOrderItemId,
        UUID pickingItemId,
        int lineNumber,
        BigDecimal quantity) {}
