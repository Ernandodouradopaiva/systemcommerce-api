package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierReturnItemRequest(
        @NotNull UUID productId,
        UUID purchaseOrderItemId,
        UUID purchaseReceiptItemId,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero") BigDecimal quantity,
        @DecimalMin(value = "0.0000") BigDecimal unitCost,
        @Size(max = 80) String batchCode,
        LocalDate expiryDate,
        @Size(max = 120) String serialNumber,
        @Size(max = 1000) String notes) {}
