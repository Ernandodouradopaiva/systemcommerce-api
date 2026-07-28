package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseReceiptCreateRequest(
        @NotNull UUID purchaseOrderId,
        @NotNull LocalDate receiptDate,
        @Size(max = 80) String invoiceNumber,
        @Size(max = 2000) String notes,
        @NotEmpty @Valid List<PurchaseReceiptItemRequest> items) {}
