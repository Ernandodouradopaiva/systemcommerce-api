package br.com.systemcommerce.supplier.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SupplierCommercialConditionRequest(
        @Min(value = 0, message = "prazo de pagamento não pode ser negativo") Integer paymentTermDays,
        @Size(max = 200) String paymentCondition,
        @Size(max = 150) String preferredCarrierName,
        @PositiveOrZero(message = "pedido mínimo não pode ser negativo") BigDecimal minOrderAmount,
        @Min(value = 0, message = "lead time não pode ser negativo") Integer averageLeadTimeDays,
        @Size(max = 2000) String notes) {}
