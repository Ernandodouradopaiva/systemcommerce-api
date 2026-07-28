package br.com.systemcommerce.pos.cancellation.dto;

import br.com.systemcommerce.pos.cancellation.entity.SaleReturn;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleReturnResponse(
        UUID id,
        String returnNumber,
        UUID originalSaleId,
        String originalSaleNumber,
        UUID cashSessionId,
        SaleReturn.Status status,
        String reason,
        String notes,
        UUID requestedById,
        String requestedByName,
        Instant confirmedAt,
        List<SaleReturnItemResponse> items,
        Long version) {

    public record SaleReturnItemResponse(
            UUID id,
            UUID productId,
            String productSku,
            String productName,
            UUID originalSaleItemId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {}
}
