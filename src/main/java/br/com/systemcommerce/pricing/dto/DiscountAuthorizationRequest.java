package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record DiscountAuthorizationRequest(
        @NotNull(message = "venda é obrigatória") UUID saleId,
        UUID saleItemId,
        @NotNull(message = "valor solicitado é obrigatório")
                @DecimalMin(value = "0.01", message = "valor solicitado deve ser maior que zero")
                BigDecimal requestedAmount,
        @Size(max = 500) String reason) {}
