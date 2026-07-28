package br.com.systemcommerce.picking.dto;

import java.util.List;
import java.util.UUID;

/** DTO compacto para impressão/coletor mobile — itens já ordenados por localização física. */
public record PickingOrderPrintDataResponse(
        UUID id,
        String pickingNumber,
        String storeCode,
        String warehouseCode,
        String salesOrderNumber,
        String status,
        List<PrintLine> lines) {

    public record PrintLine(
            UUID itemId,
            String storageLocationCode,
            String productSku,
            String productName,
            String productBarcode,
            java.math.BigDecimal quantityRequested,
            java.math.BigDecimal quantityPending) {}
}
