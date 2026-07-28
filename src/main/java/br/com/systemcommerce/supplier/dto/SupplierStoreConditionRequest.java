package br.com.systemcommerce.supplier.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SupplierStoreConditionRequest(
        @NotNull(message = "loja é obrigatória") UUID storeId,
        @Size(max = 2000) String notes,
        @Min(value = 0, message = "prazo de pagamento não pode ser negativo") Integer paymentTermDays,
        @Size(max = 200) String paymentCondition,
        @PositiveOrZero(message = "pedido mínimo não pode ser negativo") BigDecimal minOrderAmount,
        @Min(value = 0, message = "lead time não pode ser negativo") Integer averageLeadTimeDays,
        Boolean active) {}
