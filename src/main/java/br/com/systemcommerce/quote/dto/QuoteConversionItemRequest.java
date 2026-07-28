package br.com.systemcommerce.quote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Quantidade a converter de um item específico do orçamento (conversão parcial — Prompt 64). */
public record QuoteConversionItemRequest(
        @NotNull UUID quoteItemId,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero") BigDecimal quantity) {}
