package br.com.systemcommerce.stocktransfer.dto;

import br.com.systemcommerce.stocktransfer.entity.StockTransferDivergenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StockTransferDivergenceRequest(
        @NotEmpty @Valid List<DivergenceLine> items, String observation) {

    public record DivergenceLine(
            @NotNull UUID itemId,
            @NotNull @DecimalMin(value = "0.001", message = "Quantidade divergente deve ser maior que zero")
                    BigDecimal divergenceQuantity,
            String divergenceReason,
            StockTransferDivergenceType divergenceType) {

        public DivergenceLine(UUID itemId, BigDecimal divergenceQuantity, String divergenceReason) {
            this(itemId, divergenceQuantity, divergenceReason, null);
        }
    }
}
