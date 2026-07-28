package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseRequestResponse(
        UUID id,
        String requestNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        String requestingSector,
        UUID requesterUserId,
        String requesterName,
        UUID buyerUserId,
        String buyerName,
        PurchaseRequest.Priority priority,
        Instant requestedAt,
        LocalDate desiredDate,
        String justification,
        String notes,
        PurchaseRequest.PurchaseRequestStatus status,
        Boolean requiresApproval,
        String rejectionReason,
        String cancellationReason,
        List<PurchaseRequestItemResponse> items,
        boolean canEdit,
        boolean canSubmit,
        boolean canAnalyze,
        boolean canApprove,
        boolean canReject,
        boolean canCancel,
        boolean canConvert,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
