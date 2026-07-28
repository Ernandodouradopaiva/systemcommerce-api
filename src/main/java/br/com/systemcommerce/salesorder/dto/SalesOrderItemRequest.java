package br.com.systemcommerce.salesorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SalesOrderItemRequest(
        @NotNull UUID productId,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero") BigDecimal quantity,
        @DecimalMin(value = "0.00", message = "Preço unitário não pode ser negativo") BigDecimal unitPrice,
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount,
        @Size(max = 300) String description) {}
