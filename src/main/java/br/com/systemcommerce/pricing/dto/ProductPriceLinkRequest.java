package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.ProductPrice;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductPriceLinkRequest(
        @NotNull(message = "produto é obrigatório") UUID productId,
        @NotNull(message = "preço unitário é obrigatório")
                @DecimalMin(value = "0.00", message = "preço unitário não pode ser negativo")
                BigDecimal unitPrice,
        @NotNull(message = "tipo de preço é obrigatório") ProductPrice.PriceType priceType,
        @NotNull(message = "quantidade mínima é obrigatória")
                @DecimalMin(value = "0.000", message = "quantidade mínima não pode ser negativa")
                BigDecimal minQuantity,
        @NotNull(message = "prioridade é obrigatória") Integer priority,
        Instant validFrom,
        Instant validTo,
        /** Opcional na atualização; na criação o serviço força ACTIVE. */
        ProductPrice.Status status) {}
