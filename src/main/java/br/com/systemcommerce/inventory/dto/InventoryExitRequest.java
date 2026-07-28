package br.com.systemcommerce.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryExitRequest(
        @NotNull UUID productId,
        /** Quando omitido, usa o depósito padrão (LOJA-01/DEP-01). */
        UUID warehouseId,
        @NotNull @DecimalMin(value = "0.001", inclusive = true, message = "Quantidade deve ser maior que zero")
                BigDecimal quantity,
        @Size(max = 1000) String observation) {}
