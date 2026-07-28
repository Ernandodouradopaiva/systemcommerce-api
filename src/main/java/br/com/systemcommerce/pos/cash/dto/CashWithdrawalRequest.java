package br.com.systemcommerce.pos.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CashWithdrawalRequest(
        @NotNull(message = "cashSessionId é obrigatório") UUID cashSessionId,
        @NotNull @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero") BigDecimal amount,
        @NotNull(message = "motivo é obrigatório") UUID reasonId,
        @Size(max = 1000) String description,
        @Size(max = 1000) String notes,
        /** Quando a sangria exige autorização elevada, informar o usuário autorizador. */
        UUID authorizedById) {}
