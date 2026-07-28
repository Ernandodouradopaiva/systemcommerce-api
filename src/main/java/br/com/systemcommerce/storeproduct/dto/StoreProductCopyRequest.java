package br.com.systemcommerce.storeproduct.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StoreProductCopyRequest(
        @NotNull UUID sourceStoreId, @NotNull UUID targetStoreId, UUID productId) {}
