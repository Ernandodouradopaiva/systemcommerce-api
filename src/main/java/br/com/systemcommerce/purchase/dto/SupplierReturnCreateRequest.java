package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.SupplierReturn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SupplierReturnCreateRequest(
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        @NotNull UUID supplierId,
        UUID purchaseOrderId,
        UUID purchaseReceiptId,
        @NotNull SupplierReturn.ReturnReason reason,
        @Size(max = 2000) String reasonNotes,
        @NotNull SupplierReturn.OriginType originType,
        @Size(max = 2000) String notes,
        @NotEmpty @Valid List<SupplierReturnItemRequest> items) {}
