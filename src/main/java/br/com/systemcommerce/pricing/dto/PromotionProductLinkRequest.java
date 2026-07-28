package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PromotionProductLinkRequest(
        @NotNull(message = "produto é obrigatório") UUID productId,
        @NotNull(message = "preço promocional é obrigatório") @DecimalMin(value = "0.00", inclusive = false)
                BigDecimal promotionalPrice,
        @DecimalMin(value = "0.001", inclusive = true) BigDecimal minQuantity) {}
