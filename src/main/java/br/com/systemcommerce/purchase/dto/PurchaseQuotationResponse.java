package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseQuotationResponse(
        UUID id,
        String quotationNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID purchaseRequestId,
        String purchaseRequestNumber,
        UUID buyerUserId,
        String buyerName,
        Instant openedAt,
        Instant responseDeadline,
        PurchaseQuotation.PurchaseQuotationStatus status,
        PurchaseQuotation.SelectionCriteria selectionCriteria,
        Boolean autoSelectLowestPrice,
        String notes,
        Instant closedAt,
        List<PurchaseQuotationItemResponse> items,
        List<PurchaseQuotationSupplierResponse> suppliers,
        boolean canEdit,
        boolean canSend,
        boolean canRegisterResponse,
        boolean canSelectItems,
        boolean canGeneratePurchaseOrders,
        boolean canClose,
        boolean canCancel,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
