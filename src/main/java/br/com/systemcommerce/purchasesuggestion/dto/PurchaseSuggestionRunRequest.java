package br.com.systemcommerce.purchasesuggestion.dto;

import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionExecutionType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PurchaseSuggestionRunRequest(
        @NotNull UUID organizationId,
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        PurchaseSuggestionExecutionType executionType,
        Integer lookbackDays) {}
