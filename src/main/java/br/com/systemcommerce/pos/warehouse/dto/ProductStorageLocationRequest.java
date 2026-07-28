package br.com.systemcommerce.pos.warehouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductStorageLocationRequest(
        @NotNull(message = "produto é obrigatório") UUID productId,
        @NotNull(message = "localização é obrigatória") UUID storageLocationId,
        Boolean preferred,
        @DecimalMin("0") BigDecimal minQuantity,
        @DecimalMin("0") BigDecimal maxQuantity) {}
