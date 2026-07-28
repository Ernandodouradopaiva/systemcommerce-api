package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRequestItemRequest(
        UUID productId,
        @NotBlank @Size(max = 300) String description,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade solicitada deve ser maior que zero")
                BigDecimal quantityRequested,
        @Size(max = 30) String unit,
        BigDecimal currentStockInfo,
        BigDecimal minimumStock,
        @Size(max = 1000) String justification,
        UUID suggestedSupplierId) {}
