package br.com.systemcommerce.sale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemRequest(
        @NotNull UUID productId,
        @NotNull @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero") BigDecimal quantity,
        /** Preço solicitado; a API valida e pode recalcular. Null → preço de venda do produto. */
        @DecimalMin(value = "0.00", message = "Preço unitário não pode ser negativo") BigDecimal unitPrice,
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount,
        @Size(max = 200) String description) {}
