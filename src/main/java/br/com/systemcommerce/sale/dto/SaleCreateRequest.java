package br.com.systemcommerce.sale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaleCreateRequest(
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        UUID customerId,
        UUID sellerProfileId,
        UUID priceTableId,
        @Size(max = 1000) String notes) {}
