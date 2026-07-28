package br.com.systemcommerce.shipment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentItemRequest(
        @NotNull UUID productId,
        UUID salesOrderItemId,
        UUID pickingItemId,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade deve ser positiva") BigDecimal quantity) {}
