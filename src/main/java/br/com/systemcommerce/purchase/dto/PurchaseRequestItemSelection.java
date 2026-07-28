package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Seleção de item + quantidade a converter. Quantidade nula significa "todo o saldo pendente". */
public record PurchaseRequestItemSelection(
        @NotNull UUID itemId,
        @DecimalMin(value = "0.0001", message = "Quantidade a converter deve ser maior que zero")
                BigDecimal quantity) {}
