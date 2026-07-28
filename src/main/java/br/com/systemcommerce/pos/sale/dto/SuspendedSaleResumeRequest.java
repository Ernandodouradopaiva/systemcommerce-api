package br.com.systemcommerce.pos.sale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SuspendedSaleResumeRequest(
        @NotNull(message = "cashSessionId é obrigatório") UUID cashSessionId,
        Long expectedVersion,
        /** Quando true, assume a venda (troca operador) — exige permissão OTHER_OPERATOR. */
        Boolean assumeOwnership) {}
