package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.DiscountPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountPolicyCreateRequest(
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull(message = "abrangência é obrigatória") DiscountPolicy.AppliesTo appliesTo,
        UUID productId,
        UUID categoryId,
        @NotNull(message = "percentual máximo é obrigatório")
                @DecimalMin(value = "0.0000", message = "percentual máximo não pode ser negativo")
                @DecimalMax(value = "100.0000", message = "percentual máximo não pode ultrapassar 100")
                BigDecimal maxPercent,
        @DecimalMin(value = "0.00", message = "valor máximo não pode ser negativo") BigDecimal maxAmount,
        @NotNull(message = "prioridade é obrigatória") Integer priority,
        Instant validFrom,
        Instant validTo) {}
