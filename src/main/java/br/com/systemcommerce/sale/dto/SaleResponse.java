package br.com.systemcommerce.sale.dto;

import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        String saleNumber,
        UUID organizationId,
        UUID customerId,
        String customerName,
        UUID sellerId,
        String sellerName,
        UUID sellerProfileId,
        String sellerCode,
        String sellerProfileName,
        String sellerCodeSnapshot,
        String sellerNameSnapshot,
        UUID priceTableId,
        Instant saleDate,
        Sale.SaleStatus status,
        Sale.SaleChannel channel,
        UUID storeId,
        String storeCode,
        UUID terminalId,
        String terminalCode,
        UUID cashSessionId,
        UUID warehouseId,
        String warehouseCode,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal surchargeAmount,
        BigDecimal freightAmount,
        BigDecimal totalAmount,
        String notes,
        List<SaleItemResponse> items,
        boolean canEdit,
        boolean canConfirm,
        boolean canCancel,
        boolean canReceivePayment,
        boolean canSuspend,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
