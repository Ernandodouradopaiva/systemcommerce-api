package br.com.systemcommerce.picking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PickingOrderItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        String productBarcode,
        UUID storageLocationId,
        String storageLocationCode,
        Integer lineNumber,
        BigDecimal quantityRequested,
        BigDecimal quantityPicked,
        BigDecimal quantityPending,
        String barcodeScanned,
        UUID substituteProductId,
        String notes) {}
