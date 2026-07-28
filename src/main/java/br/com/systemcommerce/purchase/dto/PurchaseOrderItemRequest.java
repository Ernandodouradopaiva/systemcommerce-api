package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseOrderItemRequest(
        @NotNull UUID productId,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
                BigDecimal quantityOrdered,
        @NotNull @DecimalMin(value = "0.00", message = "Custo unitário não pode ser negativo") BigDecimal unitCost,
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount,
        @DecimalMin(value = "0.00", message = "Imposto não pode ser negativo") BigDecimal taxAmount,
        @Size(max = 300) String description,
        LocalDate expectedDate) {}
