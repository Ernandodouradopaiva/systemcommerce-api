package br.com.systemcommerce.salesorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SalesOrderCreateRequest(
        @NotNull UUID storeId,
        UUID warehouseId,
        UUID quoteId,
        UUID customerId,
        UUID sellerId,
        @Size(max = 200) String carrierName,
        @Size(max = 2000) String notes,
        Boolean reserveStock,
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount,
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo") BigDecimal freightAmount,
        @NotEmpty @Valid List<SalesOrderItemRequest> items) {}
