package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PriceTierRequest(
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade mínima deve ser maior que zero")
                BigDecimal minQuantity,
        BigDecimal maxQuantity,
        @NotNull @DecimalMin(value = "0.00", message = "Preço não pode ser negativo") BigDecimal unitPrice) {}
