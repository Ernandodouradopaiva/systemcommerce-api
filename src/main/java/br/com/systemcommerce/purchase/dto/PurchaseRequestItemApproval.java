package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRequestItemApproval(
        @NotNull UUID itemId,
        @NotNull @DecimalMin(value = "0.0000", message = "Quantidade aprovada não pode ser negativa")
                BigDecimal quantityApproved) {}
