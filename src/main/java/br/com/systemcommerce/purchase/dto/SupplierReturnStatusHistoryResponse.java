package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.SupplierReturn;
import java.time.Instant;
import java.util.UUID;

public record SupplierReturnStatusHistoryResponse(
        UUID id,
        SupplierReturn.SupplierReturnStatus fromStatus,
        SupplierReturn.SupplierReturnStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
