package br.com.systemcommerce.storeproduct.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StoreProductEnableRequest(@NotNull UUID storeId, @NotNull UUID productId) {}
