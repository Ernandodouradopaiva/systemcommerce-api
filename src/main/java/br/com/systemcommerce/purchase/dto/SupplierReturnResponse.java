package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.SupplierReturn;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupplierReturnResponse(
        UUID id,
        String returnNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        UUID supplierId,
        String supplierName,
        UUID purchaseOrderId,
        String purchaseOrderNumber,
        UUID purchaseReceiptId,
        String purchaseReceiptNumber,
        SupplierReturn.ReturnReason reason,
        String reasonNotes,
        SupplierReturn.SupplierReturnStatus status,
        SupplierReturn.OriginType originType,
        Instant dispatchedAt,
        Instant completedAt,
        String notes,
        List<SupplierReturnItemResponse> items,
        boolean canEdit,
        boolean canSubmit,
        boolean canApprove,
        boolean canReject,
        boolean canDispatch,
        boolean canComplete,
        boolean canCancel,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
