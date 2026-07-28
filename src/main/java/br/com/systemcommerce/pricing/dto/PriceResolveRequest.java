package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Corpo do simulador oficial de resolução de preço — {@code POST /api/v1/price-tables/resolve} (Prompt 68). */
public record PriceResolveRequest(
        @NotNull UUID productId,
        UUID storeId,
        BigDecimal quantity,
        PriceChannel channel,
        UUID customerId,
        String customerGroupCode) {}
