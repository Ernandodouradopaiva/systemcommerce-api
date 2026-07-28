package br.com.systemcommerce.sale.dto;

import br.com.systemcommerce.sale.entity.SaleItem;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productBarcode,
        String productName,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal lineSubtotal,
        BigDecimal lineTotal,
        BigDecimal availableStock,
        SaleItem.PriceSource priceSource,
        UUID priceTableId,
        UUID productPriceId,
        UUID discountAuthorizedById) {}
