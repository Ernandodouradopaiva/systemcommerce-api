package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseReceiptResponse(
        UUID id,
        String receiptNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        UUID purchaseOrderId,
        String purchaseOrderNumber,
        UUID supplierId,
        String supplierName,
        LocalDate receiptDate,
        String invoiceNumber,
        String invoiceSeries,
        String accessKey,
        LocalDate invoiceIssuedAt,
        String carrierName,
        String notes,
        PurchaseReceipt.PurchaseReceiptStatus status,
        UUID receivedByUserId,
        String receivedByName,
        Instant postedAt,
        UUID postedByUserId,
        List<PurchaseReceiptItemResponse> items,
        boolean canInspect,
        boolean canAccept,
        boolean canPostToInventory,
        boolean canReject,
        boolean canCancel,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
