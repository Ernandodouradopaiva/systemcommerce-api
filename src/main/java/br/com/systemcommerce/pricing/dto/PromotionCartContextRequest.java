package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PromotionCartContextRequest(
        @NotNull(message = "loja é obrigatória") UUID storeId,
        PriceChannel channel,
        UUID customerId,
        String customerGroupCode,
        String couponCode,
        @NotEmpty(message = "itens do carrinho são obrigatórios") @Valid List<PromotionCartItemRequest> items) {}
