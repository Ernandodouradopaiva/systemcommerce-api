package br.com.systemcommerce.inventory.dto;

import br.com.systemcommerce.inventory.entity.InventoryMovement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryAdjustmentRequest(
        @NotNull UUID productId,
        /** Quando omitido, usa o depósito padrão (LOJA-01/DEP-01). */
        UUID warehouseId,
        @NotNull @DecimalMin(value = "0.001", inclusive = true, message = "Quantidade deve ser maior que zero")
                BigDecimal quantity,
        /**
         * ADJUSTMENT_POSITIVE, ADJUSTMENT_NEGATIVE ou CORRECTION.
         * Para CORRECTION, informe {@code effect}.
         */
        @NotNull InventoryMovement.MovementType type,
        /** Obrigatório quando type = CORRECTION. */
        StockEffect effect,
        @NotNull UUID reasonId,
        @Size(max = 1000) String observation) {

    public enum StockEffect {
        INCREASE,
        DECREASE
    }
}
