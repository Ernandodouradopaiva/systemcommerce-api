package br.com.systemcommerce.sale.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record SaleFreightRequest(
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo") BigDecimal freightAmount,
        @DecimalMin(value = "0.00", message = "Acréscimo não pode ser negativo") BigDecimal surchargeAmount) {}
