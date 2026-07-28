package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.Promotion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromotionCreateRequest(
        UUID organizationId,
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull(message = "canal é obrigatório") PriceChannel channel,
        @NotNull(message = "prioridade é obrigatória") Integer priority,
        Instant validFrom,
        Instant validTo,
        Promotion.PromotionType promotionType,
        Boolean stackable,
        @DecimalMin(value = "0", message = "valor mínimo do pedido não pode ser negativo") BigDecimal minOrderAmount,
        UUID brandId,
        UUID categoryId) {}
