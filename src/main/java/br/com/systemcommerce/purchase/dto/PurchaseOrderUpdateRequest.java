package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderUpdateRequest(
        UUID destinationStoreId,
        @NotNull UUID warehouseId,
        @NotNull UUID supplierId,
        UUID buyerUserId,
        LocalDate expectedDate,
        @Size(max = 2000) String notes,
        @Size(max = 200) String paymentCondition,
        @Size(max = 200) String carrierName,
        @Size(max = 40) String freightModality,
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount,
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo") BigDecimal freightAmount,
        @DecimalMin(value = "0.00", message = "Imposto não pode ser negativo") BigDecimal taxAmount,
        @DecimalMin(value = "0.00", message = "Seguro não pode ser negativo") BigDecimal insuranceAmount,
        @DecimalMin(value = "0.00", message = "Despesas não podem ser negativas") BigDecimal expenseAmount,
        Boolean allowOverReceipt,
        @NotEmpty @Valid List<PurchaseOrderItemRequest> items) {}
