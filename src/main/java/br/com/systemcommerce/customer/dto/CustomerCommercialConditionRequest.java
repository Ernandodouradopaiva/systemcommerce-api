package br.com.systemcommerce.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CustomerCommercialConditionRequest(
        @Min(value = 0, message = "prazo de pagamento não pode ser negativo") Integer paymentTermDays,
        @Size(max = 100) String paymentCondition,
        UUID priceTableId,
        @Size(max = 1000) String notes) {}
