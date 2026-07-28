package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseRequestConvertRequest(
        @Valid List<PurchaseRequestItemSelection> items,
        List<UUID> supplierIds,
        Instant responseDeadline,
        @Size(max = 2000) String notes) {}
