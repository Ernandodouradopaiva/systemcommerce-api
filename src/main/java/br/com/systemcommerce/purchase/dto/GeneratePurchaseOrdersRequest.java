package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record GeneratePurchaseOrdersRequest(
        @NotNull UUID warehouseId, LocalDate expectedDate, @Size(max = 2000) String notes) {}
