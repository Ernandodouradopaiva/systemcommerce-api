package br.com.systemcommerce.pos.sale.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PosSaleStartRequest(
        @NotNull(message = "cashSessionId é obrigatório") UUID cashSessionId, UUID sellerProfileId) {

    public PosSaleStartRequest(UUID cashSessionId) {
        this(cashSessionId, null);
    }
}
