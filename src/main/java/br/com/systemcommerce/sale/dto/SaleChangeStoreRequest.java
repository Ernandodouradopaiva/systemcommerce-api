package br.com.systemcommerce.sale.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaleChangeStoreRequest(@NotNull UUID newStoreId, @NotNull UUID newWarehouseId) {}
