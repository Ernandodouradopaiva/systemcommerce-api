package br.com.systemcommerce.supplier.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SupplierProductRequest(
        @NotNull(message = "produto é obrigatório") UUID productId,
        @Size(max = 60) String supplierSku,
        @PositiveOrZero(message = "preço de compra não pode ser negativo") BigDecimal lastPurchasePrice,
        @Min(value = 0, message = "lead time não pode ser negativo") Integer leadTimeDays,
        Boolean active) {}
