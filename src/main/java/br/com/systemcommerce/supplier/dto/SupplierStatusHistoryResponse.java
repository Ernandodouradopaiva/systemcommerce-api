package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.Supplier;
import java.time.Instant;
import java.util.UUID;

public record SupplierStatusHistoryResponse(
        UUID id,
        UUID supplierId,
        Supplier.SupplierStatus fromStatus,
        Supplier.SupplierStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedByUserId,
        String changedByUserName) {}
