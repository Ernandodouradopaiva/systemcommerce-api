package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseRequestUpdateRequest(
        UUID warehouseId,
        @Size(max = 120) String requestingSector,
        UUID requesterUserId,
        UUID buyerUserId,
        PurchaseRequest.Priority priority,
        LocalDate desiredDate,
        @Size(max = 2000) String justification,
        @Size(max = 2000) String notes,
        @NotEmpty @Valid List<PurchaseRequestItemRequest> items) {}
