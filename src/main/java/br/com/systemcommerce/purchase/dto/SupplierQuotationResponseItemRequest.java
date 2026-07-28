package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SupplierQuotationResponseItemRequest(
        @NotNull UUID quotationItemId,
        @NotNull @DecimalMin(value = "0.0000", message = "Preço unitário não pode ser negativo") BigDecimal unitPrice,
        BigDecimal quantityAvailable,
        @DecimalMin(value = "0.00") BigDecimal freightAmount,
        @DecimalMin(value = "0.00") BigDecimal taxAmount,
        @DecimalMin(value = "0.00") BigDecimal discountAmount,
        Integer leadTimeDays,
        @Size(max = 120) String brandOffered,
        @Size(max = 1000) String notes) {}
