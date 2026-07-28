package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import java.time.Instant;
import java.util.UUID;

public record PurchaseQuotationStatusHistoryResponse(
        UUID id,
        PurchaseQuotation.PurchaseQuotationStatus fromStatus,
        PurchaseQuotation.PurchaseQuotationStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
