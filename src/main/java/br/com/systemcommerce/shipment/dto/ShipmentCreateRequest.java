package br.com.systemcommerce.shipment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Criação de expedição a partir de pedido de venda/separação — parcial é permitido (Prompt 72). */
public record ShipmentCreateRequest(
        @NotNull UUID salesOrderId,
        UUID pickingOrderId,
        UUID warehouseId,
        UUID carrierId,
        UUID freightModeId,
        String carrierName,
        String freightModeLabel,
        BigDecimal freightAmount,
        LocalDate expectedDelivery,
        String notes,
        @NotEmpty @Valid List<ShipmentItemRequest> items) {}
