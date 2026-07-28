package br.com.systemcommerce.sale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SaleDiscountRequest(
        @NotNull @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount) {}
