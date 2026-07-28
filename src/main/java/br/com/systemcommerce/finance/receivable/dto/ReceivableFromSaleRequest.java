package br.com.systemcommerce.finance.receivable.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReceivableFromSaleRequest(
        @NotNull UUID saleId,
        UUID paymentConditionId,
        UUID financialCategoryId,
        UUID costCenterId,
        String idempotencyKey) {}
