package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import java.time.Instant;
import java.util.UUID;

public record PurchaseReceiptStatusHistoryResponse(
        UUID id,
        PurchaseReceipt.PurchaseReceiptStatus fromStatus,
        PurchaseReceipt.PurchaseReceiptStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
