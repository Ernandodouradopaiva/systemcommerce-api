package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseQuotationCreateRequest(
        UUID purchaseRequestId,
        @NotNull UUID storeId,
        UUID buyerUserId,
        Instant responseDeadline,
        PurchaseQuotation.SelectionCriteria selectionCriteria,
        Boolean autoSelectLowestPrice,
        @Size(max = 2000) String notes,
        List<UUID> supplierIds,
        @NotEmpty @Valid List<PurchaseQuotationItemRequest> items) {}
