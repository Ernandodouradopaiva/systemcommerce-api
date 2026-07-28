package br.com.systemcommerce.salesorder.dto;

import br.com.systemcommerce.salesorder.entity.SalesOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SalesOrderResponse(
        UUID id,
        String orderNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        UUID quoteId,
        UUID customerId,
        String customerName,
        UUID sellerId,
        String sellerName,
        String carrierName,
        SalesOrder.SalesOrderStatus status,
        String notes,
        boolean reserveStock,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal freightAmount,
        BigDecimal totalAmount,
        UUID generatedSaleId,
        List<SalesOrderItemResponse> items,
        boolean canEdit,
        boolean canCancel,
        boolean canGenerateSale,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
