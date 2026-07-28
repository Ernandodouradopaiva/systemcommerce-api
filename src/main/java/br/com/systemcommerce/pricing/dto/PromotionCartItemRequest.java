package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PromotionCartItemRequest(
        @NotNull(message = "produto é obrigatório") UUID productId,
        UUID categoryId,
        UUID brandId,
        @NotNull(message = "quantidade é obrigatória") @DecimalMin(value = "0.001", message = "quantidade deve ser maior que zero")
                BigDecimal quantity,
        @NotNull(message = "preço unitário é obrigatório") @DecimalMin(value = "0", message = "preço unitário não pode ser negativo")
                BigDecimal unitPrice) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
