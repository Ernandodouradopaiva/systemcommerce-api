package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PurchaseRequestPartialApprovalRequest(
        @NotEmpty @Valid List<PurchaseRequestItemApproval> items, @Size(max = 1000) String notes) {}
