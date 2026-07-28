package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import java.time.Instant;
import java.util.UUID;

public record PurchaseRequestStatusHistoryResponse(
        UUID id,
        PurchaseRequest.PurchaseRequestStatus fromStatus,
        PurchaseRequest.PurchaseRequestStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
