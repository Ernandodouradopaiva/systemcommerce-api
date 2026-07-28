package br.com.systemcommerce.finance.payable.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PayableFromPurchaseRequest(
        @NotNull UUID purchaseReceiptId,
        UUID paymentConditionId,
        UUID financialCategoryId,
        UUID costCenterId,
        String idempotencyKey) {}
