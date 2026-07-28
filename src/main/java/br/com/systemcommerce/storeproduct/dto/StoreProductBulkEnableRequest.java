package br.com.systemcommerce.storeproduct.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record StoreProductBulkEnableRequest(
        @NotNull UUID productId, @NotEmpty List<UUID> storeIds) {}
