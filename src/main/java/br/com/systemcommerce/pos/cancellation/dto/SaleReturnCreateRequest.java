package br.com.systemcommerce.pos.cancellation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaleReturnCreateRequest(
        @NotNull(message = "venda original é obrigatória") UUID originalSaleId,
        UUID cashSessionId,
        @NotBlank(message = "motivo é obrigatório") @Size(max = 500) String reason,
        @Size(max = 1000) String notes,
        @NotEmpty(message = "informe ao menos um item") @Valid List<SaleReturnItemRequest> items) {

    public record SaleReturnItemRequest(
            @NotNull(message = "produto é obrigatório") UUID productId,
            UUID originalSaleItemId,
            @NotNull @DecimalMin(value = "0.001", message = "quantidade deve ser positiva") BigDecimal quantity) {}
}
