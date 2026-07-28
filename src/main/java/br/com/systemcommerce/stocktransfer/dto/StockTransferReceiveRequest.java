package br.com.systemcommerce.stocktransfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StockTransferReceiveRequest(
        @NotEmpty @Valid List<ReceiveLine> items, String observation, String idempotencyKey) {

    public record ReceiveLine(
            @NotNull UUID itemId,
            @NotNull @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
                    BigDecimal quantity) {}
}
