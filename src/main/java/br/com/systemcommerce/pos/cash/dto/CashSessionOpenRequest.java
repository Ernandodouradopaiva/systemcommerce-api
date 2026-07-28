package br.com.systemcommerce.pos.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CashSessionOpenRequest(
        @NotNull(message = "terminal é obrigatório") UUID terminalId,
        @NotNull(message = "valor inicial é obrigatório")
                @DecimalMin(value = "0.00", message = "Valor inicial não pode ser negativo")
                BigDecimal openingAmount,
        @Size(max = 1000) String openingNotes) {}
